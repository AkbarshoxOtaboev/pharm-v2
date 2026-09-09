package uz.uwon.pharm.users;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String fullName;
    private String username;
    private String password;
    private String personalPhone;
    private String workPhone;
    private String avatar;
    private Role role;
}
