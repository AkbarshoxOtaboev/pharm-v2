package uz.uwon.pharm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uz.uwon.pharm.users.Role;
import uz.uwon.pharm.users.User;
import uz.uwon.pharm.users.UserRepository;
import uz.uwon.pharm.utils.Status;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class DefaultAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:}")
    private String username;

    @Value("${app.admin.password:}")
    private String password;

    @Value("${app.admin.full-name:Administrator}")
    private String fullName;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return;
        }
        String login = username.trim();
        if (userRepository.existsByUsername(login)) {
            log.info("Default admin '{}' already exists", login);
            return;
        }
        userRepository.save(User.builder()
                .username(login)
                .password(passwordEncoder.encode(password))
                .fullName(StringUtils.hasText(fullName) ? fullName.trim() : "Administrator")
                .avatar("default-1")
                .role(Role.ADMIN)
                .status(Status.ACTIVE)
                .build());
        log.info("Default admin '{}' created", login);
    }
}
