package com.focaquest.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.focaquest.auth.AuthDtos.LoginRequest;
import com.focaquest.auth.AuthDtos.MeResponse;
import com.focaquest.auth.AuthDtos.RegisterRequest;
import com.focaquest.auth.AuthDtos.UpdateProfileRequest;
import com.focaquest.shared.CurrentUser;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Autenticação e perfil")
public class AuthController {

    private final AuthService auth;
    private final TokenService tokens;
    private final AuthProperties properties;

    public AuthController(AuthService auth, TokenService tokens, AuthProperties properties) {
        this.auth = auth;
        this.tokens = tokens;
        this.properties = properties;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<MeResponse> register(@Valid @RequestBody RegisterRequest req) {
        AppUser user = auth.register(req);
        return withSession(HttpStatus.CREATED, user);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<MeResponse> login(@Valid @RequestBody LoginRequest req) {
        AppUser user = auth.authenticate(req.email(), req.password());
        return withSession(HttpStatus.OK, user);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = cookie("", java.time.Duration.ZERO);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @GetMapping("/me")
    public MeResponse me() {
        return MeResponse.of(auth.get(CurrentUser.id()));
    }

    @PatchMapping("/me")
    public MeResponse update(@Valid @RequestBody UpdateProfileRequest req) {
        return MeResponse.of(auth.updateProfile(CurrentUser.id(), req));
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail badCredentials(BadCredentialsException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        pd.setTitle("Credenciais inválidas");
        return pd;
    }

    private ResponseEntity<MeResponse> withSession(HttpStatus status, AppUser user) {
        ResponseCookie cookie = cookie(tokens.issue(user), tokens.ttl());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(MeResponse.of(user));
    }

    private ResponseCookie cookie(String value, java.time.Duration maxAge) {
        return ResponseCookie.from(SecurityConfig.SESSION_COOKIE, value)
                .httpOnly(true)
                .secure(properties.secureCookie())
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
