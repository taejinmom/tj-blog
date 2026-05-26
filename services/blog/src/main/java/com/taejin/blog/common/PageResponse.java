package com.taejin.blog.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Spring Data {@link Page} 를 직접 직렬화하면 버전에 따라 응답 구조가 불안정하므로,
 * 프론트엔드와 합의된 고정 형태로 매핑한다.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> p) {
        return new PageResponse<>(
                p.getContent(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages(),
                p.isLast()
        );
    }
}
