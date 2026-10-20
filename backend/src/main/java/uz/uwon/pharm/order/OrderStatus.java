package uz.uwon.pharm.order;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

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

    /** Orders whose items are taken from the courier stock but not yet delivered. */
    public static final Set<OrderStatus> ACTIVE = EnumSet.of(NEW, CONFIRMED, PROCESSING, SHIPPED, PENDING);

    private final String label;
    OrderStatus(String label) {
        this.label = label;
    }
}
