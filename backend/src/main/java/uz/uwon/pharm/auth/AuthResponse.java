package uz.uwon.pharm.auth;

import lombok.Builder;
import lombok.Data;
import uz.uwon.pharm.users.Role;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long userId;
    private String username;
    private String fullName;
    private String avatar;
    private Role role;
}
