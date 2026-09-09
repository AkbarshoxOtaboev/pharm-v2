package uz.uwon.pharm.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
    private long accessTokenMs = 86_400_000L;
    private long refreshTokenMs = 604_800_000L;
}
