package com.siren.notificationservice.telegram.service;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.telegram.config.TelegramSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramMessageService {

    private final TelegramSender adminTelegramSender;
    private final TelegramSender memberTelegramSender;

    /**
     * 딥링크 토큰이 만료됐거나 이미 사용됐을 때 재발급 안내 메시지를 보낸다.
     */
    public void sendTokenExpiredMessage(String chatId, BotType botType) {
        String text = "링크가 만료되었거나 이미 사용되었습니다. 앱에서 텔레그램 연동을 다시 요청해 주세요";
        sendMessage(chatId, botType, text, "링크 만료 안내");
    }

    /**
     * 텔레그램 연동이 성공했을 때 환영 메시지를 보낸다.
     */
    public void sendLinkSuccessMessage(String chatId, BotType botType) {
        String text = "환영합니다! 성공적으로 연동되었습니다.";
        sendMessage(chatId, botType, text, "연동 성공 안내");
    }

    /**
     * Admin 봇에 자유 텍스트가 온 경우, Member 봇으로 이동하라는 안내와 딥링크를 보낸다.
     */
    public void sendRedirectToUserBotMessage(String chatId, String deepLinkUrl) {
        String text = "4iren-운영진 봇은 질의응답을 제공하지 않습니다. 전체 이용가능한 봇으로 이동하세요\n" + deepLinkUrl;
        sendMessage(chatId, BotType.ADMIN_BOT, text, "운영진 봇 안내");
    }

    /**
     * 텍스트가 아닌 메시지(사진/스티커 등)를 받았을 때 지원하지 않는다는 안내를 보낸다.
     */
    public void sendUnsupportedContentMessage(String chatId, BotType botType) {
        String text = "죄송합니다, 지금은 텍스트 메시지만 이해할 수 있습니다.";
        sendMessage(chatId,botType, text, "지원하지 않는 콘텐츠 안내");
    }

    /**
     * 의도분류 결과가 FALLBACK일 때(무슨 말인지 못 알아들었을 때) 안내 메시지를 보낸다.
     */
    public void sendFallbackMessage(String chatId, BotType botType) {
        String text = "음, 무슨 말씀인지 잘 이해하지 못했어요. 강의실 환경이 어떤지 물어보시거나(예: \"지금 온도 어때?\"), 느끼신 걸 편하게 남겨주세요(예: \"너무 더워요\")!";
        sendMessage(chatId,botType,text,"자연어처리에 대한 fallback메시지");
    }

    /**
     * 아직 텔레그램 연동이 안 된 유저가 메시지를 보냈을 때 연동 안내를 보낸다.
     */
    public void sendNotLinkedGuideMessage(String chatId, BotType botType) {
        String text = "아직 텔레그램 연동이 안 되어 있어요. 웹에서 먼저 연동을 진행해 주세요.";
        sendMessage(chatId, botType, text, "미연동 안내");
    }

    /**
     * Core API 응답 실패(서킷브레이커 open 등)로 조회범위를 못 가져왔을 때 안내를 보낸다.
     */
    public void sendCoreApiUnavailableMessage(String chatId, BotType botType) {
        String text = "지금은 확인이 어려워요, 잠시 후 다시 시도해주세요.";
        sendMessage(chatId, botType, text, "Core API 응답 실패 안내");
    }

    /**
     * 실제로 텔레그램에 메시지를 발송한다. 발송 실패는 예외를 삼키고 로그만 남긴다 (재시도 없음).
     * @param chatId 발송하려고 하는 chatId
     * @param botType 발송 챗 봇 타입
     * @param text 발송하려고 하는 메시지
     * @param context 로그 구분용 라벨 (예: "링크 만료 안내", "연동 성공 안내")
     */
    public void sendMessage(String chatId, BotType botType, String text, String context) {
        SendMessage message = new SendMessage(chatId, text);
        try {
            resolveTelegramSender(botType).execute(message);
        } catch (TelegramApiRequestException e) {
            if (Integer.valueOf(403).equals(e.getErrorCode())) {
                log.info("{} 발송 불가 - 봇 차단 상태 (botType={}, chatId={})", context, botType, chatId);
            } else {
                log.warn("{} 발송 실패 (botType={}, chatId={}, errorCode={})", context, botType, chatId, e.getErrorCode(), e);
            }
        } catch (TelegramApiException e) {
            log.warn("{} 발송 실패 (botType={}, chatId={})", context, botType, chatId, e);
        }
    }

    /**
     * 봇에 따른 텔레그램 sender 선택
     * @param botType Admin 봇인지 User 봇인지
     * @return memberTelegramSender / adminTelegramSender
     */
    private TelegramSender resolveTelegramSender(BotType botType) {
        return switch (botType) {
            case USER_BOT -> memberTelegramSender;
            case ADMIN_BOT -> adminTelegramSender;
        };
    }
}
