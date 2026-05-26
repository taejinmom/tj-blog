package com.taejin.blog.exception;

/**
 * 요청한 리소스(게시물/로드맵 항목 등)가 존재하지 않을 때 던진다.
 * GlobalExceptionHandler 에서 404 Not Found 로 변환된다.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
