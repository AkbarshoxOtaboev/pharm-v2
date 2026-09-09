package uz.uwon.pharm.users;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uz.uwon.pharm.exceptions.NotFoundException;
import uz.uwon.pharm.order.OrderRepository;
import uz.uwon.pharm.utils.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class UserServiceImplement implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderRepository orderRepository;

    @Override
    public void save(UserDto dto) {
        log.info("Saving user: username {}", dto.getUsername());
        User user = User.builder()
                .fullName(dto.getFullName())
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .personalPhone(dto.getPersonalPhone())
                .workPhone(dto.getWorkPhone())
                .avatar(StringUtils.hasText(dto.getAvatar()) ? dto.getAvatar() : "default-1")
                .role(dto.getRole())
                .status(Status.ACTIVE)
                .build();
        userRepository.save(user);
    }

    @Override
    public UserResponse findByUsername(String username) {
        log.info("Find user by username {}", username);
        return mapToUserResponse(userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found!")));
    }

    @Override
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::mapToUserResponse).collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> findUsersByRole(Role role) {
        return userRepository.findUsersByRoleAndStatus(role, Status.ACTIVE).stream().map(this::mapToUserResponse).collect(Collectors.toList());
    }

    @Override
    public void update(String username, UserDto dto) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found!"));
        user.setFullName(dto.getFullName());
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        user.setPersonalPhone(dto.getPersonalPhone());
        user.setWorkPhone(dto.getWorkPhone());
        if (StringUtils.hasText(dto.getAvatar())) {
            user.setAvatar(dto.getAvatar());
        }
        user.setRole(dto.getRole());
    }

    @Override
    public void delete(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found!"));
        user.setStatus(Status.DELETED);
    }

    @Override
    public void blockUser(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found!"));
        user.setStatus(Status.BLOCKED);
    }

    @Override
    public void unBlockUser(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found!"));
        user.setStatus(Status.ACTIVE);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public UserResponse getCurrentUser() {
        return findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Override
    public UserStatResponse getUserStats() {
        return userRepository.getUserStatistics();
    }

    @Override
    public CourierStatsDto getCourierStatsDto(
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        LocalDateTime start;
        LocalDateTime end;

        if (startDate != null && endDate != null) {
            start = startDate;
            end = endDate;
        } else {
            LocalDateTime now = LocalDateTime.now();

            start = now.withDayOfMonth(1)
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            end = start.plusMonths(1); // следующий месяц 00:00
        }

        return orderRepository.getCourierStats(userId, start, end);
    }

    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getPersonalPhone(),
                user.getWorkPhone(),
                StringUtils.hasText(user.getAvatar()) ? user.getAvatar() : "default-1",
                user.getRole(),
                user.getStatus()
        );
    }
}
