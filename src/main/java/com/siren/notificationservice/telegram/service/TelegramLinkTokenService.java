package com.siren.notificationservice.telegram.service;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.repository.TelegramSubscriptionRepository;
import com.siren.notificationservice.telegram.config.TelegramBotProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
    private final TelegramSubscriptionRepository telegramSubscriptionRepository;
    private final TelegramBotProperties telegramBotProperties;
    private static final String DEEP_LINK_BASE_URL="https://t.me/";
    private static final String DEEP_LINK_START_PARAM ="?start=";

    public String getDeepLinkUrl(Long userId, BotType botType) {
        String uuid = issueToken(userId, botType);
        String botUsername = switch (botType) {
            case ADMIN_BOT -> telegramBotProperties.adminBot().username();
            case USER_BOT -> telegramBotProperties.memberBot().username();
        };
        return DEEP_LINK_BASE_URL + botUsername + DEEP_LINK_START_PARAM + uuid;
    }
    /**
     * 딥 링크 연동 시 UUID를 발급
     * @param userId 사용자 아이디
     * @param botType 어떤 봇으로 연동할건지
     * @return uuid token(String)
     */
    private String issueToken(Long userId, BotType botType) {
        String token = UUID.randomUUID().toString();
        String redisKey = "telegram:link-token:" + botType.name() + ":" + token;

        stringRedisTemplate.opsForValue().set(redisKey, userId.toString(), LINK_TOKEN_TTL);
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
    public Optional<Long> consumeToken(String token, BotType botType) {
        String redisKey = "telegram:link-token:" + botType.name() + ":" + token;
        String userId = stringRedisTemplate.opsForValue().getAndDelete(redisKey);

        if(userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        try{
            return Optional.of(Long.parseLong(userId));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * 특정 유저가 특정 봇에 이미 연동돼 있는지 확인한다.
     * 프론트가 딥링크 토큰 발급 전 "이미 연동되어 있습니다, 재연동하시겠어요?" 확인
     * 다이얼로그를 보여줄지 판단하는 데 쓴다.
     *
     * @param userId  대상 유저 id
     * @param botType ADMIN_BOT 또는 USER_BOT
     * @return 연동 여부
     */
    public boolean isLinked(Long userId, BotType botType) {
        return telegramSubscriptionRepository.existsByUserIdAndBotType(userId, botType);
    }
}
