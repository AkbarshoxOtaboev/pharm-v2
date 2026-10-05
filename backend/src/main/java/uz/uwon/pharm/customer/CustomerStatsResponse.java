package uz.uwon.pharm.customer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uz.uwon.pharm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class CustomerStatsResponse {
    private Long id;
    private String fullName;
    private String phone;
    private LocalDate birthDate;
    private String description;
    private Gender gender;
    private CustomerType type;
    private Status status;
    private LocalDate createdAt;
    private long orderCount;
    private BigDecimal orderSum;
    private BigDecimal avgCheck;
}
