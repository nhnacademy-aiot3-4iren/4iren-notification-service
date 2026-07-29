package com.siren.notificationservice.telegram.service;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.telegram.callback.CallbackActionType;
import com.siren.notificationservice.telegram.config.TelegramSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.List;

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
     * 되묻기 답변이 후보 강의실과 매칭 안 됐을 때, 후보 목록을 다시 보여주며 재질문한다.
     */
    public void sendRoomDisambiguationRetryMessage(String chatId, BotType botType, List<String> roomNames) {
        String text = "음, 어느 강의실인지 잘 모르겠어요. 아래에서 다시 골라주세요.";
        sendInlineKeyboardMessage(chatId, botType, text, CallbackActionType.FEEDBACK_ROOM_SELECT, roomNames);
    }

    /**
     * 구독한 강의실이 하나도 없는 유저가 피드백을 남기려 할 때 안내한다.
     */
    public void sendNoSubscribedRoomMessage(String chatId, BotType botType) {
        String text = "현재 구독한 강의실이 없어요. 먼저 강의실을 구독해 주세요.";
        sendMessage(chatId,botType,text,"구독 강의실 없음 안내");
    }

    /**
     * 구독 강의실이 여러 개라 어느 강의실 피드백인지 모호할 때, 후보 목록을 보여주며 되묻는다.
     */
    public void sendRoomDisambiguationAskMessage(String chatId, BotType botType, List<String> roomNames) {
        String text = "구독하신 강의실 목록 중에서 어느 강의실 얘기이신가요?";
        sendInlineKeyboardMessage(chatId, botType, text, CallbackActionType.FEEDBACK_ROOM_SELECT, roomNames);
    }
    /**
     * 피드백이 접수됐을 때 소프트 확언을 보낸다. 실제 조치를 확언하는 게 아니라
     * "의견을 받았다"는 것만 알린다 — 온도를 바꿔주겠다는 식의 약속이 아님.
     */
    public void sendFeedbackAcknowledgeMessage(String chatId, BotType botType) {
        String text = "의견을 반영하여 더 나은 강의실 환경을 만들겠습니다.";
        sendMessage(chatId, botType, text, "피드백 접수 확언");
    }

    /**
     * 피드백 처리 큐 publish 실패 등, 내부 처리 단계에서 문제가 생겼을 때 안내한다.
     * Core API 실패({@link #sendCoreApiUnavailableMessage})와 구분되는 별도 원인이라 전용 메서드로 분리.
     */
    public void sendFeedbackProcessingFailedMessage(String chatId, BotType botType) {
        String text = "지금은 의견을 접수하기 어려워요, 잠시 후 다시 시도해주세요.";
        sendMessage(chatId, botType, text, "피드백 처리 실패 안내");
    }

    /**
     * 인라인 키보드 탭에 응답한다 — 안 부르면 텔레그램 클라이언트에 로딩 스피너가 계속 돈다.
     * 콜백 처리 성공/실패와 무관하게 항상 먼저(또는 처리 직후) 호출해야 한다.
     */
    public void answerCallback(String callbackQueryId, BotType botType) {
        AnswerCallbackQuery answer = AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQueryId)
                .build();
        try {
            resolveTelegramSender(botType).execute(answer);
        } catch (TelegramApiException e) {
            log.warn("콜백 응답 실패 (botType={}, callbackQueryId={})", botType, callbackQueryId, e);
        }
    }

    /**
     * 실제로 텔레그램에 메시지를 발송한다. 발송 실패는 예외를 삼키고 로그만 남긴다 (재시도 없음).
     * @param chatId 발송하려고 하는 chatId
     * @param botType 발송 챗 봇 타입
     * @param text 발송하려고 하는 메시지
     * @param context 로그 구분용 라벨 (예: "링크 만료 안내", "연동 성공 안내")
     */
    public void sendMessage(String chatId, BotType botType, String text, String context) {
        executeSendMessage(botType, new SendMessage(chatId, text), chatId, context);
    }

    /**
     * 선택지를 인라인 키보드 버튼으로 보여주고 발송한다. 각 버튼의 callback_data는
     * "{actionType.prefix()}:{선택지 텍스트}" 형식 — 탭하면 그 선택지 텍스트가 그대로
     * TelegramInboundEvent.question()으로 돌아오게 만드는 규칙(CallbackActionType 참고).
     */
    public void sendInlineKeyboardMessage(String chatId, BotType botType, String text,
                                          CallbackActionType actionType, List<String> options) {
        List<List<InlineKeyboardButton>> keyboard = options.stream()
                .map(option -> InlineKeyboardButton.builder()
                        .text(option)
                        .callbackData(actionType.prefix() + ":" + option)
                        .build())
                .map(List::of) // 한 줄에 버튼 하나씩 - 강의실 이름 길이가 들쭉날쭉해도 안전
                .toList();

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(keyboard).build())
                .build();

        executeSendMessage(botType, message, chatId, "인라인 키보드");
    }

    /**
     * {sendMessage}/{sendInlineKeyboardMessage}가 공유하는 발송 실행부.
     * 발송 실패는 예외를 삼키고 로그만 남긴다 (재시도 없음).
     */
    private void executeSendMessage(BotType botType, SendMessage message, String chatId, String context) {
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
