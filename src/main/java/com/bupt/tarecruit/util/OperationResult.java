package com.bupt.tarecruit.util;

import java.util.Optional;

public record OperationResult<T>(boolean success, String message, T data) {

    public static <T> OperationResult<T> success(T data, String message) {
        return new OperationResult<>(true, message, data);
    }

    public static <T> OperationResult<T> success(T data) {
        return new OperationResult<>(true, "", data);
    }

    public static <T> OperationResult<T> failure(String message) {
        return new OperationResult<>(false, message, null);
    }

    public Optional<T> dataOptional() {
        return Optional.ofNullable(data);
    }
}
