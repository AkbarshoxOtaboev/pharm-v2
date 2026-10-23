package uz.uwon.pharm.customer;

public enum CustomerSalesSegment {
    ORDERED,
    NONE,
    CANCELLED;

    public static CustomerSalesSegment from(String raw) {
        if (raw == null || raw.isBlank()) return ORDERED;
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ORDERED;
        }
    }
}
