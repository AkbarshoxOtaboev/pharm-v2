package uz.uwon.pharm.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uz.uwon.pharm.exceptions.NotFoundException;
import uz.uwon.pharm.users.User;
import uz.uwon.pharm.users.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String DEFAULT_AVATAR = "default-1";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenBlacklist tokenBlacklist;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtService.isValid(refreshToken) || tokenBlacklist.isBlacklisted(refreshToken)) {
            throw new IllegalArgumentException("Refresh token yaroqsiz");
        }
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return buildAuthResponse(user);
    }

    public void logout(String accessToken) {
        if (accessToken != null && jwtService.isValid(accessToken)) {
            tokenBlacklist.add(accessToken, jwtService.getRemainingTtlMs(accessToken));
        }
    }

    public MeResponse me() {
        return toMe(currentUser());
    }

    public MeResponse updateProfile(UpdateProfileRequest request) {
        User user = currentUser();
        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName().trim());
        }
        user.setPersonalPhone(blankToNull(request.getPersonalPhone()));
        user.setWorkPhone(blankToNull(request.getWorkPhone()));
        if (StringUtils.hasText(request.getAvatar())) {
            user.setAvatar(request.getAvatar().trim());
        }
        return toMe(user);
    }

    public void changePassword(ChangePasswordRequest request) {
        User user = currentUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Joriy parol noto'g'ri");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private MeResponse toMe(User user) {
        return MeResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .personalPhone(user.getPersonalPhone())
                .workPhone(user.getWorkPhone())
                .avatar(resolveAvatar(user.getAvatar()))
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user.getUsername()))
                .refreshToken(jwtService.generateRefreshToken(user.getUsername()))
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatar(resolveAvatar(user.getAvatar()))
                .role(user.getRole())
                .build();
    }

    public static String resolveAvatar(String avatar) {
        return StringUtils.hasText(avatar) ? avatar : DEFAULT_AVATAR;
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
