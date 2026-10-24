package com.pipelinegen.exception;

/** Стек определён, но шаблона для него (или для выбранной CI-системы) пока нет. */
public class UnsupportedStackException extends RuntimeException {
    public UnsupportedStackException(String message) {
        super(message);
    }
}
