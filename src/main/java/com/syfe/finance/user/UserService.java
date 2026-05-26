package com.syfe.finance.user;

import com.syfe.finance.auth.CurrentUser;
import com.syfe.finance.auth.LoginRequest;
import com.syfe.finance.auth.RegisterRequest;
import com.syfe.finance.auth.RegisterResponse;
import com.syfe.finance.common.ApiException;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw ApiException.conflict("Username is already registered");
        }
        UserAccount user = new UserAccount(
                username,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                request.phoneNumber().trim()
        );
        UserAccount saved = userRepository.save(user);
        return new RegisterResponse("User registered successfully", saved.getId());
    }

    @Transactional(readOnly = true)
    public Long authenticate(LoginRequest request) {
        String username = normalizeUsername(request.username());
        UserAccount user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> ApiException.unauthorized("Invalid username or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid username or password");
        }
        return user.getId();
    }

    @Transactional(readOnly = true)
    public UserAccount requireUser(CurrentUser currentUser) {
        if (currentUser == null) {
            throw ApiException.unauthorized("Authentication required");
        }
        return userRepository.findById(currentUser.id())
                .orElseThrow(() -> ApiException.unauthorized("Authenticated user no longer exists"));
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
