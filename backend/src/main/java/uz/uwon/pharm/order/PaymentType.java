package uz.uwon.pharm.order;

import lombok.Getter;

@Getter
public enum PaymentType {
    CASH("Наличный"),
    CARD("Карта");
    private final String label;
    PaymentType (String label){
        this.label= label;
    }
}
