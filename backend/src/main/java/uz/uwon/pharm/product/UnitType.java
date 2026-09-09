package uz.uwon.pharm.product;

import lombok.Getter;

@Getter
public enum UnitType {
    PCS("Шт"),
    BOX("Каробка"),
    KG("Кг");
    private final String label;

    UnitType(String label) {
        this.label = label;
    }
}
