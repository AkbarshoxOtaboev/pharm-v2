package uz.uwon.pharm.order;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import uz.uwon.pharm.common.ApiResponse;
import uz.uwon.pharm.common.PageMeta;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
public class OrdersApiController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<List<OrderResponse>> list(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        OrderPageResponse<OrderResponse> result =
                orderService.getOrders(id, phone, status, fromDate, toDate, page, size, userId);
        PageMeta meta = PageMeta.builder()
                .page(result.getPage())
                .size(size)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
        return ApiResponse.ok(result.getContent(), meta);
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.orderDetailResponses(id));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody OrderDTO dto) {
        orderService.save(dto);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody OrderDTO dto) {
        orderService.update(id, dto);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        orderService.updateStatus(id, body.get("status"));
        return ApiResponse.ok(null);
    }
}
