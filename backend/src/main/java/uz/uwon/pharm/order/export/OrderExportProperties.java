package uz.uwon.pharm.order.export;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Ixtiyoriy sozlama. Asosiy katalog /admin/products (DB) dan olinadi.
 * {@code productIds} berilsa — faqat shu tartibdagi mahsulotlar Excel ustunlariga tushadi.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.order-export")
public class OrderExportProperties {

    /**
     * Excel Mahsulotlar ustunlari tartibi (max 9 → F..N).
     * Bo'sh bo'lsa — barcha ACTIVE mahsulotlar sortNumber bo'yicha.
     */
    private List<Long> productIds = new ArrayList<>();

    /**
     * Sovg'alar ustunlari (max 8 → Q..X).
     * Bo'sh bo'lsa — productIds / mahsulotlarning birinchi 8 tasi.
     */
    private List<Long> giftProductIds = new ArrayList<>();

    @Getter
    @Setter
    public static class ProductColumn {
        private String column;
        private String title;
        private BigDecimal price = BigDecimal.ZERO;
        private Long productId;
        private List<String> aliases = new ArrayList<>();
    }
}
