package com.bupt.tarecruit.util;

import java.util.Optional;

/**
 * Outcome wrapper for service-layer operations that may succeed or fail with a message.
 *
 * @param <T> type of optional payload data on success
 * @param success whether the operation completed successfully
 * @param message user-facing or diagnostic message (may be empty on success)
 * @param data    result payload; typically {@code null} on failure
 */
public record OperationResult<T>(boolean success, String message, T data) {

    /**
     * Creates a successful result with data and a message.
     *
     * @param data    result payload
     * @param message success message
     * @param <T>     payload type
     * @return successful operation result
     */
    public static <T> OperationResult<T> success(T data, String message) {
        return new OperationResult<>(true, message, data);
    }

    /**
     * Creates a successful result with data and an empty message.
     *
     * @param data result payload
     * @param <T>  payload type
     * @return successful operation result
     */
    public static <T> OperationResult<T> success(T data) {
        return new OperationResult<>(true, "", data);
    }

    /**
     * Creates a failed result with no data.
     *
     * @param message failure message
     * @param <T>     payload type
     * @return failed operation result
     */
    public static <T> OperationResult<T> failure(String message) {
        return new OperationResult<>(false, message, null);
    }

    /**
     * Returns the payload as an {@link Optional}, empty when {@code data} is null.
     *
     * @return optional payload
     */
    public Optional<T> dataOptional() {
        return Optional.ofNullable(data);
    }
}
