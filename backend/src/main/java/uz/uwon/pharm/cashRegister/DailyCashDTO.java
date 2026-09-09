package uz.uwon.pharm.cashRegister;

import java.time.LocalDate;

public interface DailyCashDTO {
    LocalDate getDate();
    Double getTotal();
}
