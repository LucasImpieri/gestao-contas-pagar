package com.impieri.gestaocontaspagar.dto;

public record LoginResponse(
        String token,
        String type
) {
}