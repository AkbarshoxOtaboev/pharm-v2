package uz.uwon.pharm.order.export;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.uwon.pharm.product.Product;
import uz.uwon.pharm.product.ProductRepository;
import uz.uwon.pharm.utils.Status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class OrderExportCatalogFactory {

    private static final String[] PRODUCT_COLS = {"F", "G", "H", "I", "J", "K", "L", "M", "N"};
    private static final String[] GIFT_COLS = {"Q", "R", "S", "T", "U", "V", "W", "X"};

    /**
     * TZ / namuna Excel tartibi — /admin/products nomlariga mos kalitlar.
     * Mos kelmagan mahsulotlar oxiriga sortNumber bo'yicha qo'shiladi.
     */
    private static final String[] PREFERRED_ORDER = {
            "чой",
            "райхон",
            "эликсир", "еликсир",
            "орегадо", "алато",
            "ёстиқ", "йостик", "естик",
            "оне", "воншот", "one shot",
            "ванна",
            "кора", "қора",
            "чуфа"
    };

    private final ProductRepository productRepository;
    private final OrderExportProperties properties;

    public OrderExportCatalog build() {
        List<Product> active = productRepository.findAllProductByStatusOrderBySortNumberAsc(Status.ACTIVE);

        List<Product> paidProducts = pickProducts(properties.getProductIds(), active, PRODUCT_COLS.length);
        List<Product> giftSource = properties.getGiftProductIds() != null && !properties.getGiftProductIds().isEmpty()
                ? pickProducts(properties.getGiftProductIds(), active, GIFT_COLS.length)
                : paidProducts;
        // Sovg'alarda Чуфа yo'q (TZ): oxirgi "чуфа" ni chiqarib tashlash
        List<Product> giftProducts = giftSource.stream()
                .filter(p -> !normalize(p.getName()).contains("чуфа"))
                .limit(GIFT_COLS.length)
                .toList();

        return new OrderExportCatalog(
                toColumns(paidProducts, PRODUCT_COLS),
                toColumns(giftProducts, GIFT_COLS)
        );
    }

    private List<Product> pickProducts(List<Long> ids, List<Product> fallbackOrdered, int limit) {
        if (ids != null && !ids.isEmpty()) {
            List<Product> picked = new ArrayList<>();
            for (Long id : ids) {
                if (picked.size() >= limit) break;
                Product p = fallbackOrdered.stream()
                        .filter(x -> Objects.equals(x.getId(), id))
                        .findFirst()
                        .orElse(null);
                if (p != null) picked.add(p);
            }
            return picked;
        }
        return orderByPreferred(fallbackOrdered, limit);
    }

    private static List<Product> orderByPreferred(List<Product> products, int limit) {
        List<Product> remaining = new ArrayList<>(products);
        List<Product> ordered = new ArrayList<>();
        for (String key : PREFERRED_ORDER) {
            String k = normalize(key);
            Product found = null;
            for (Product p : remaining) {
                if (normalize(p.getName()).contains(k)) {
                    found = p;
                    break;
                }
            }
            final Product matched = found;
            if (matched != null && ordered.stream().noneMatch(p -> Objects.equals(p.getId(), matched.getId()))) {
                ordered.add(matched);
                remaining.remove(matched);
            }
            if (ordered.size() >= limit) return ordered;
        }
        for (Product p : remaining) {
            if (ordered.size() >= limit) break;
            ordered.add(p);
        }
        return ordered;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replace('қ', 'к')
                .replace('ғ', 'г')
                .replace('ў', 'у')
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "");
    }

    private static List<OrderExportCatalog.Column> toColumns(List<Product> products, String[] letters) {
        List<OrderExportCatalog.Column> cols = new ArrayList<>();
        for (int i = 0; i < products.size() && i < letters.length; i++) {
            Product p = products.get(i);
            BigDecimal price = p.getPriceCost() != null ? p.getPriceCost()
                    : (p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO);
            cols.add(new OrderExportCatalog.Column(
                    letters[i],
                    p.getName() != null ? p.getName() : "",
                    price,
                    p.getId()
            ));
        }
        return cols;
    }
}
