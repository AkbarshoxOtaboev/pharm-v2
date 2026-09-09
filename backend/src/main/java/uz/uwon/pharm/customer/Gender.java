package uz.uwon.pharm.customer;

import lombok.Getter;

@Getter
public enum Gender {
    MALE("Мужчина"),
    FEMALE("Женшина"),
    NOT_SELECTED("Не указан");
    private final String label;
    Gender(String label){
        this.label =label;
    }

}
