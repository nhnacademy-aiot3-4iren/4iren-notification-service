package com.siren.notificationservice.telegram.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.domain.UserRole;
import com.siren.notificationservice.core.entity.table.TelegramSubscription;
import com.siren.notificationservice.core.service.basic_service.TelegramSubscriptionService;
import com.siren.notificationservice.telegram.config.TelegramBotProperties;
import com.siren.notificationservice.telegram.dto.LinkTokenData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramLinkTokenService {

    /**
     * 딥링크 토큰 TTL(분). Duration은 애노테이션 값(컴파일 타임 상수)으로 못 쓰기 때문에,
     * Swagger 설명(@Operation)처럼 상수를 그대로 문서화해야 하는 곳은 이 primitive를 참조한다.
     */
    public static final long LINK_TOKEN_TTL_MINUTES = 5L;

    /**
     * 딥링크 토큰의 Redis TTL이자 프론트에 알려주는 만료 시각의 기준값.
     * 발급(issueToken)과 응답 표시(DeepLinkController) 양쪽이 이 상수 하나만 참조하도록 해서,
     * TTL을 바꿀 때 한 곳만 고치면 되게 한다.
     */
    public static final Duration LINK_TOKEN_TTL = Duration.ofMinutes(LINK_TOKEN_TTL_MINUTES);

    private final StringRedisTemplate stringRedisTemplate;
    private final TelegramSubscriptionService telegramSubscriptionService;
    private final TelegramBotProperties telegramBotProperties;
    private final ObjectMapper objectMapper;
    private static final String DEEP_LINK_BASE_URL="https://t.me/";
    private static final String DEEP_LINK_START_PARAM ="?start=";

    /**
     * 사용자에게 딥링크를 제공해줍니다
     * @param botType 제공할 딥링크의 봇
     * @return 완성된 딥링크 전체본
     */
    public String getDeepLinkUrl(LinkTokenData linkTokenData, BotType botType) {
        String uuid = issueToken(linkTokenData, botType);
        String botUsername = resolveBotUsername(botType);
        return DEEP_LINK_BASE_URL + botUsername + DEEP_LINK_START_PARAM + uuid;
    }

    /**
     * Admin이 admin 봇에서 실수로 자연어를 보낼때 member 딥링크를 보냅니다 그 과정에서 이미 member에 연동되어있는 admin은
     * redirect url만 주고 member에 연동되지 않은 admin일 경우는 연동할 수 있는 딥링크를 제공해줍니다.
     * @param userId admin userId (따로 검증할 필요없음 admin 봇에서 제공해줌)
     * @param botType 제공할 딥링크의 봇
     * @return 리다이렉트 url
     */
    public String getRedirectUrl(Long userId, BotType botType) {
        boolean alreadyLinked = telegramSubscriptionService.isLinked(userId, botType);
        if(alreadyLinked) {
            return DEEP_LINK_BASE_URL + resolveBotUsername(botType);
        }
        List<TelegramSubscription> ts = telegramSubscriptionService.getTelegramSubscriptions(userId);
        UserRole role = ts.stream().map(TelegramSubscription::getUserRole).findFirst().orElse(UserRole.NORMAL);
        return getDeepLinkUrl(new LinkTokenData(userId, role), botType);
    }
    /**
     * 딥 링크 연동 시 UUID를 발급
     */
    private String issueToken(LinkTokenData data, BotType botType) {
        String token = UUID.randomUUID().toString();
        String redisKey = linkTokenKey(botType, token);
        try{
            String value = objectMapper.writeValueAsString(data);
            stringRedisTemplate.opsForValue().set(redisKey, value, LINK_TOKEN_TTL);
        }catch (JsonProcessingException e) {
            throw new IllegalStateException("링크 토큰 직렬화 실패", e); // 사실상 안 나지만 방어
        }
        return token;
    }

    /**
     * 토큰을 조회와 동시에 삭제(1회용)하여 매핑된 userId를 반환한다.
     * 토큰이 없거나 만료됐거나 값이 손상된 경우 빈 Optional을 반환한다.
     *
     * @param token   검증할 토큰
     * @param botType 어떤 봇으로 연동 중인지
     * @return 토큰에 매핑된 userId, 없으면 empty
     */
    public Optional<LinkTokenData> consumeToken(String token, BotType botType) {
        String redisKey = linkTokenKey(botType, token);
        String value = stringRedisTemplate.opsForValue().getAndDelete(redisKey);

        if(value == null || value.isBlank()) {
            return Optional.empty();
        }
        try{
            return Optional.of(objectMapper.readValue(value, LinkTokenData.class));
        } catch (JsonProcessingException e) {
            log.warn("링크 토큰 역직렬화 실패(손상된 값), 무시", e);
            return Optional.empty();
        }
    }


    private String linkTokenKey(BotType botType, String token) {
        return "telegram:link-token:" + botType.name() + ":" + token;
    }
    /**
     * 봇 타입에 해당하는 텔레그램 봇 username을 반환한다.
     *
     * @param botType ADMIN_BOT 또는 USER_BOT
     * @return 해당 봇의 username
     */
    private String resolveBotUsername(BotType botType) {
        return switch (botType) {
            case ADMIN_BOT -> telegramBotProperties.adminBot().username();
            case USER_BOT -> telegramBotProperties.memberBot().username();
        };
    }
}
