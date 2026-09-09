package uz.uwon.pharm.courierStock;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import uz.uwon.pharm.common.ApiResponse;
import uz.uwon.pharm.courierStockHistory.CourierStockHistoryResponse;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/courier-stock")
@RequiredArgsConstructor
@Tag(name = "Courier Stock")
public class CourierStockApiController {

    private final CourierStockService courierStockService;

    @PostMapping("/transfer")
    public ApiResponse<Void> transfer(@RequestBody CourierStockDTO dto) {
        courierStockService.save(dto);
        return ApiResponse.ok(null);
    }

    @PostMapping("/return")
    public ApiResponse<Void> returnToStore(@RequestBody ReturnDTO dto) {
        courierStockService.returnToStore(dto);
        return ApiResponse.ok(null);
    }

    @GetMapping("/products/search")
    public ApiResponse<List<CourierStockResponse>> productSearch(
            @RequestParam Long courierId,
            @RequestParam String q) {
        return ApiResponse.ok(courierStockService.productSearch(courierId, q));
    }

    @GetMapping("/{courierId}")
    public ApiResponse<List<CourierStockResponse>> list(@PathVariable Long courierId) {
        return ApiResponse.ok(courierStockService.courierStocks(courierId));
    }

    @GetMapping("/{courierId}/history")
    public ApiResponse<List<CourierStockHistoryResponse>> history(
            @PathVariable Long courierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(courierStockService.stockHistoryResponse(courierId, from, to));
    }
}
