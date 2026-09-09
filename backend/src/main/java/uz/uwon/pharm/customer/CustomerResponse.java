package uz.uwon.pharm.customer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import uz.uwon.pharm.utils.Status;

import java.time.LocalDate;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
public class CustomerResponse {
    private Long id;
    private String fullName;
    private String phone;
    private LocalDate birthDate;
    private String description;
    private Gender gender;
    private CustomerType type;
    private Status status;
    private LocalDate createdAt;
}
