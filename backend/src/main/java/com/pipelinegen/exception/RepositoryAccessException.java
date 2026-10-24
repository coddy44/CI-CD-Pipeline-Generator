package com.pipelinegen.exception;

/** Репозиторий не найден, приватный или не удалось склонировать. Бросает анализатор. */
public class RepositoryAccessException extends RuntimeException {
    public RepositoryAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
