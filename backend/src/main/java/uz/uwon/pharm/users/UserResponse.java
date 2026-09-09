package uz.uwon.pharm.users;

import lombok.Getter;
import lombok.Setter;
import uz.uwon.pharm.utils.Status;

@Getter
@Setter
public class UserResponse {
    private Long id;
    private String fullName;
    private String username;
    private String personalPhone;
    private String workPhone;
    private String avatar;
    private Role role;
    private Status status;

    public UserResponse(Long id, String fullName, String username, String personalPhone, String workPhone, String avatar, Role role, Status status) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.personalPhone = personalPhone;
        this.workPhone = workPhone;
        this.avatar = avatar;
        this.role = role;
        this.status = status;
    }

    public UserResponse(Long id, String fullName, String username, String personalPhone, String workPhone, Role role, Status status) {
        this(id, fullName, username, personalPhone, workPhone, null, role, status);
    }

    public UserResponse(Long id, String workPhone, String fullName) {
        this.id = id;
        this.workPhone = workPhone;
        this.fullName = fullName;
    }
}
