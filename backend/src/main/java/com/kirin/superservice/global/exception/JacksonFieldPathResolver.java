package com.kirin.superservice.global.exception;

import java.util.List;

import tools.jackson.core.JacksonException;

public final class JacksonFieldPathResolver {

    private JacksonFieldPathResolver() {
    }

    public static String resolve(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof JacksonException jacksonException) {
                String path = formatPath(jacksonException.getPath());
                if (path != null) {
                    return path;
                }
            }
        }
        return null;
    }

    static String formatPath(List<JacksonException.Reference> references) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference reference : references) {
            if (reference.getPropertyName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.isEmpty() ? null : path.toString();
    }
}
