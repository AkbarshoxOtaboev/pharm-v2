package uz.uwon.pharm.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    private String fullName;
    private String personalPhone;
    private String workPhone;
    private String avatar;
}
