package uz.uwon.pharm.order.export;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Excel Mahsulotlar / Sovg'alar ustunlari — /admin/products ro'yxatidan. */
public record OrderExportCatalog(
        List<Column> products,
        List<Column> gifts
) {
    public record Column(
            String column,
            String title,
            BigDecimal price,
            Long productId
    ) {}

    public static OrderExportCatalog empty() {
        return new OrderExportCatalog(List.of(), List.of());
    }

    public List<OrderExportProperties.ProductColumn> productColumns() {
        return toProps(products);
    }

    public List<OrderExportProperties.ProductColumn> giftColumns() {
        return toProps(gifts);
    }

    private static List<OrderExportProperties.ProductColumn> toProps(List<Column> cols) {
        List<OrderExportProperties.ProductColumn> out = new ArrayList<>(cols.size());
        for (Column c : cols) {
            OrderExportProperties.ProductColumn p = new OrderExportProperties.ProductColumn();
            p.setColumn(c.column());
            p.setTitle(c.title());
            p.setPrice(c.price() != null ? c.price() : BigDecimal.ZERO);
            p.setProductId(c.productId());
            out.add(p);
        }
        return out;
    }
}
