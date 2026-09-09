package uz.uwon.pharm.auth;

import lombok.Builder;
import lombok.Data;
import uz.uwon.pharm.users.Role;
import uz.uwon.pharm.utils.Status;

@Data
@Builder
public class MeResponse {
    private Long id;
    private String username;
    private String fullName;
    private String personalPhone;
    private String workPhone;
    private String avatar;
    private Role role;
    private Status status;
}
