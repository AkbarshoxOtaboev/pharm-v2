package uz.uwon.pharm.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username majburiy")
    private String username;

    @NotBlank(message = "Parol majburiy")
    private String password;
}
