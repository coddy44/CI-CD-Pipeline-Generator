package com.pipelinegen.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * Единый формат ошибки для всех эндпоинтов.
 *
 * @param code      машинный код ошибки (фронт может по нему выбирать текст)
 * @param message   описание для пользователя
 * @param details   подробности (например, какие поля не прошли валидацию)
 * @param timestamp время ошибки
 */
public record ErrorResponse(String code, String message, List<String> details, Instant timestamp) {

    public static ErrorResponse of(String code, String message, List<String> details) {
        return new ErrorResponse(code, message, details, Instant.now());
    }

    public static ErrorResponse of(String code, String message) {
        return of(code, message, List.of());
    }
}
