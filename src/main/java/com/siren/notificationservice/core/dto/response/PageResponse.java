package com.siren.notificationservice.core.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이지 응답 계약. Spring PageImpl의 JSON은 필드명/구조가 불안정해서, 프론트가 역직렬화하는
 * PageResponse 모양(content/page/size/totalElements/totalPages/first/last)에 맞춰 직접 정의한다.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    /**
     * Spring Data Page를 와이어 계약용 PageResponse로 변환한다.
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
