package uz.uwon.pharm.users;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uz.uwon.pharm.common.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UsersApiController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<List<UserResponse>> list() {
        return ApiResponse.ok(userService.findAll());
    }

    @GetMapping("/couriers")
    public ApiResponse<List<UserResponse>> couriers() {
        return ApiResponse.ok(userService.findUsersByRole(Role.COURIER));
    }

    @GetMapping("/check-username")
    public ApiResponse<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        return ApiResponse.ok(Map.of("exists", userService.existsByUsername(username)));
    }

    @GetMapping("/statistics")
    public ApiResponse<UserStatResponse> statistics() {
        return ApiResponse.ok(userService.getUserStats());
    }

    @GetMapping("/{username}")
    public ApiResponse<UserResponse> get(@PathVariable String username) {
        return ApiResponse.ok(userService.findByUsername(username));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody UserDto dto) {
        userService.save(dto);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{username}")
    public ApiResponse<Void> update(@PathVariable String username, @Valid @RequestBody UserDto dto) {
        userService.update(username, dto);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{username}")
    public ApiResponse<Void> delete(@PathVariable String username) {
        userService.delete(username);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{username}/block")
    public ApiResponse<Void> block(@PathVariable String username) {
        userService.blockUser(username);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{username}/unblock")
    public ApiResponse<Void> unblock(@PathVariable String username) {
        userService.unBlockUser(username);
        return ApiResponse.ok(null);
    }
}
