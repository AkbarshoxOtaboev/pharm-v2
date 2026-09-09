package uz.uwon.pharm.order;

import lombok.Getter;

@Getter
public enum OrderStatus {
    NEW("Новый"),
    CONFIRMED("Подтверждено"),
    PROCESSING("Обработка"),
    SHIPPED("Отправлено"),
    PENDING("В ожидании"),
    DELIVERED("Доставлено"),
    CANCELLED("Отменено"),
    DELETED("Удалено");
    private final String label;
    OrderStatus(String label) {
        this.label = label;
    }
}
