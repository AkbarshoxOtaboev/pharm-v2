package uz.uwon.pharm.order.export;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.uwon.pharm.location.Region;
import uz.uwon.pharm.location.RegionRepository;
import uz.uwon.pharm.order.Order;
import uz.uwon.pharm.order.OrderRepository;
import uz.uwon.pharm.orderitem.OrderItem;
import uz.uwon.pharm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderExportService {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("dd_MM_yyyy");

    private final OrderRepository orderRepository;
    private final RegionRepository regionRepository;
    private final OrderExportCatalogFactory catalogFactory;

    public record ExportFile(String filename, String sheetRegion, byte[] content) {}

    @Transactional(readOnly = true)
    public ExportFile export(Long regionId, LocalDate date) {
        if (regionId == null || date == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "regionId and date are required");
        }
        Region region = regionRepository.findByIdAndStatus(regionId, Status.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found"));

        OrderExportCatalog catalog = catalogFactory.build();
        if (catalog.products().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active products for Excel columns");
        }

        List<Order> orders = orderRepository.findForExcelExport(
                regionId,
                date.atStartOfDay(),
                date.atTime(23, 59, 59)
        );
        List<OrderExportRow> rows = orders.stream().map(o -> toRow(o, catalog)).toList();

        try {
            byte[] bytes = OrderExcelWorkbookBuilder.build(
                    region.getName(),
                    date,
                    rows,
                    catalog.productColumns(),
                    catalog.giftColumns()
            );
            String shortName = shortRegion(region.getName()).replace(' ', '_');
            String filename = shortName + "_" + date.format(FILE_DATE) + ".xlsx";
            return new ExportFile(filename, shortName, bytes);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Excel export failed: " + e.getMessage(), e);
        }
    }

    private OrderExportRow toRow(Order order, OrderExportCatalog catalog) {
        String name = order.getCustomer() != null ? nullToEmpty(order.getCustomer().getFullName()) : "";
        String phone = order.getCustomer() != null ? toNineDigits(order.getCustomer().getPhone()) : "";
        String address = buildAddress(order);

        Map<String, BigDecimal> items = new LinkedHashMap<>();
        Map<String, BigDecimal> gifts = new LinkedHashMap<>();

        List<OrderItem> orderItems = order.getOrderItems() != null ? order.getOrderItems() : List.of();
        for (OrderItem item : orderItems) {
            if (item.getProduct() == null || item.getProduct().getId() == null) continue;
            Long productId = item.getProduct().getId();
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            boolean bonus = Boolean.TRUE.equals(item.getIsBonus());

            OrderExportCatalog.Column col = bonus
                    ? findByProductId(catalog.gifts(), productId)
                    : findByProductId(catalog.products(), productId);

            // Bonus mahsulot sovg'a ustunida yo'q bo'lsa — mahsulotlar ro'yxatidan topib, gifts ga bir xil productId bo'yicha
            if (col == null && bonus) {
                OrderExportCatalog.Column paid = findByProductId(catalog.products(), productId);
                if (paid != null) {
                    col = findByProductId(catalog.gifts(), paid.productId());
                }
            }
            if (col == null) continue;

            Map<String, BigDecimal> target = bonus ? gifts : items;
            target.merge(col.column(), qty, BigDecimal::add);
        }

        String specialist = "";
        if (order.getUser() != null) {
            String full = nullToEmpty(order.getUser().getFullName());
            specialist = order.getUser().getId() != null
                    ? order.getUser().getId() + " " + full
                    : full;
        }

        return new OrderExportRow(
                name,
                address,
                phone,
                1,
                items,
                gifts,
                nullToEmpty(order.getOrientations()),
                specialist.trim()
        );
    }

    private static OrderExportCatalog.Column findByProductId(List<OrderExportCatalog.Column> cols, Long productId) {
        if (cols == null || productId == null) return null;
        for (OrderExportCatalog.Column c : cols) {
            if (Objects.equals(c.productId(), productId)) return c;
        }
        return null;
    }

    private static String buildAddress(Order order) {
        List<String> parts = new ArrayList<>();
        if (order.getAddress() != null && !order.getAddress().isBlank()) parts.add(order.getAddress().trim());
        if (order.getHome() != null && !order.getHome().isBlank()) parts.add("uy: " + order.getHome().trim());
        if (order.getEntrance() != null && !order.getEntrance().isBlank()) parts.add("podyezd: " + order.getEntrance().trim());
        if (order.getApartment() != null && !order.getApartment().isBlank()) parts.add("xonadon: " + order.getApartment().trim());
        if (order.getFloor() != null && !order.getFloor().isBlank()) parts.add("qavat: " + order.getFloor().trim());
        return String.join(", ", parts);
    }

    static String toNineDigits(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() >= 9) {
            return digits.substring(digits.length() - 9);
        }
        return digits;
    }

    private static String shortRegion(String name) {
        if (name == null || name.isBlank()) return "Region";
        return name.replace(" viloyati", "").replace(" Вилояти", "").trim();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
