package uz.uwon.pharm.customer;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class CustomerDTO {
    private String fullName;
    private String phone;
    private LocalDate birthDate;
    private String description;
    private Gender gender;
    private CustomerType type;
}
