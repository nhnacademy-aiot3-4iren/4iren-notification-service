package com.siren.notificationservice.core.dto;

/**
 * 대화 메모리를 Redis에 저장하기 위한 최소 단위.
 * Spring AI의 Message는 인터페이스(UserMessage/AssistantMessage 등 여러 구현체)라
 * Jackson으로 직접 역/직렬화하려면 다형성 타입 정보가 필요해 번거롭다
 * role/content만 담는 이 record로 변환해서 저장하고, 읽을 때 다시 Message로 복원한다.
 */
public record ChatMemoryEntry(
        String role,
        String content
) {
}
