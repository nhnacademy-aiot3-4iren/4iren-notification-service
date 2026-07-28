package com.siren.notificationservice.core.dto;

/**
 * QUESTION 라우트의 마지막 질문 및 답변 한쌍. Recommendation API에 후속 정정 맥락으로 실어보내기 위한 DTO임
 * @param question 유저 질문
 * @param answer chat bot의 답변
 */
public record LastQuestionAnswer(
        String question,
        String answer
) {
}
