package com.impieri.gestaocontaspagar.web;

import com.impieri.gestaocontaspagar.dto.LoginRequest;
import com.impieri.gestaocontaspagar.dto.LoginResponse;
import com.impieri.gestaocontaspagar.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        if (!"admin".equals(request.username()) || !"admin123".equals(request.password())) {
            throw new IllegalArgumentException("Usuário ou senha inválidos");
        }

        String token = jwtService.gerarToken(request.username());

        return ResponseEntity.ok(new LoginResponse(token, "Bearer"));
    }
}