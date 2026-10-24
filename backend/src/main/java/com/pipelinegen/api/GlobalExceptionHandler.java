package com.pipelinegen.api;

import com.pipelinegen.api.dto.ErrorResponse;
import com.pipelinegen.exception.RepositoryAccessException;
import com.pipelinegen.exception.UnsupportedStackException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/** Переводит исключения в единый JSON-формат ErrorResponse с нужным HTTP-статусом. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException e) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();
        return ErrorResponse.of("VALIDATION_ERROR", "Некорректные данные запроса", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnreadable(HttpMessageNotReadableException e) {
        return ErrorResponse.of("MALFORMED_REQUEST",
                "Тело запроса не JSON или содержит неизвестное значение (например, ciPlatform)");
    }

    @ExceptionHandler(RepositoryAccessException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ErrorResponse handleRepository(RepositoryAccessException e) {
        return ErrorResponse.of("REPOSITORY_NOT_ACCESSIBLE", e.getMessage());
    }

    @ExceptionHandler(UnsupportedStackException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ErrorResponse handleUnsupported(UnsupportedStackException e) {
        return ErrorResponse.of("UNSUPPORTED_STACK", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleOther(Exception e) {
        log.error("Необработанная ошибка", e);
        return ErrorResponse.of("INTERNAL_ERROR", "Внутренняя ошибка сервиса");
    }
}
