package com.taejin.blog.exception;

/** 인증은 됐으나 해당 동작 권한이 없을 때. GlobalExceptionHandler 에서 403 으로 변환. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
