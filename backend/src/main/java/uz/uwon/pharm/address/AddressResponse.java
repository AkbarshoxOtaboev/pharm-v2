package uz.uwon.pharm.address;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uz.uwon.pharm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
@Getter
@AllArgsConstructor
public class AddressResponse {
    private Long id;
    private String fullAddress;
    private String home;
    private String entrance;
    private String apartment;
    private String floor;
    private String orientation;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Status status;
    private LocalDate createAt;
}
