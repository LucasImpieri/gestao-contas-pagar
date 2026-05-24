package com.impieri.gestaocontaspagar.web.exception;

import java.time.OffsetDateTime;
import java.util.Map;

public record ApiValidationError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fields
) {
}