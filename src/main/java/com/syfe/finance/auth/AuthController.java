package com.syfe.finance.auth;

import com.syfe.finance.common.MessageResponse;
import com.syfe.finance.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public MessageResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        Long userId = userService.authenticate(request);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(SessionKeys.AUTH_USER_ID, userId);
        httpRequest.changeSessionId();
        return new MessageResponse("Login successful");
    }

    @PostMapping("/logout")
    public MessageResponse logout(@AuthenticationPrincipal CurrentUser currentUser, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return new MessageResponse("Logout successful");
    }
}
