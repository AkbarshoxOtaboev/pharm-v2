package uz.uwon.pharm.address;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AddressDTO {
    private String fullAddress;
    private String home;
    private String entrance;
    private String apartment;
    private String floor;
    private String orientation;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
