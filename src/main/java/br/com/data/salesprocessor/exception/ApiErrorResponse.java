package br.com.data.salesprocessor.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> fields
) {

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(LocalDateTime.now(), status, error, message, path, List.of());
    }

    public static ApiErrorResponse withFields(
            int status,
            String error,
            String message,
            String path,
            List<FieldErrorDetail> fields
    ) {
        return new ApiErrorResponse(LocalDateTime.now(), status, error, message, path, fields);
    }
}
