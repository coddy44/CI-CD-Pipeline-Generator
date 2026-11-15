package com.pipelinegen.exception;

/** Проект не подходит для анализа или для выбранной CI-системы нет шаблона. */
public class UnsupportedStackException extends RuntimeException {
    public UnsupportedStackException(String message) {
        super(message);
    }
}
