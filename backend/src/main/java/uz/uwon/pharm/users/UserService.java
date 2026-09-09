package uz.uwon.pharm.users;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface UserService {
    void save(UserDto dto);
    UserResponse findByUsername(String username);
    List<UserResponse> findAll();
    List<UserResponse> findUsersByRole(Role role);
    void update(String username, UserDto dto);
    void delete(String username);
    void blockUser(String username);
    void unBlockUser(String username);
    boolean existsByUsername(String username);
    UserResponse getCurrentUser();
    UserStatResponse getUserStats();
    CourierStatsDto getCourierStatsDto(Long userId, LocalDateTime startDate,
                                       LocalDateTime endDate);
}
