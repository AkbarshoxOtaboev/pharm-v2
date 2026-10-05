package uz.uwon.pharm.customer;

import uz.uwon.pharm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CustomerStatsView {
    Long getId();
    String getFullName();
    String getPhone();
    LocalDate getBirthDate();
    String getDescription();
    Gender getGender();
    CustomerType getType();
    Status getStatus();
    LocalDate getCreatedAt();
    Long getOrderCount();
    BigDecimal getOrderSum();
}
