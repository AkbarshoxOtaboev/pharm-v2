package uz.uwon.pharm.cashRegister;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import uz.uwon.pharm.common.ApiResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cash-register")
@RequiredArgsConstructor
@Tag(name = "Cash Register")
public class CashRegisterApiController {

    private final CashRegisterService cashRegisterService;

    @GetMapping("/courier/{courierId}")
    public ApiResponse<List<CashRegisterResponse>> byCourier(
            @PathVariable Long courierId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return ApiResponse.ok(cashRegisterService.findCashRegistersByCourierId(courierId, dateFrom, dateTo));
    }

    @GetMapping("/courier/{courierId}/total")
    public ApiResponse<Map<String, BigDecimal>> total(@PathVariable Long courierId) {
        return ApiResponse.ok(Map.of("total", cashRegisterService.totalCourierCash(courierId)));
    }

    @PatchMapping("/{id}/return")
    public ApiResponse<Void> returnCash(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String comment = body != null ? body.get("comment") : null;
        cashRegisterService.returnCash(id, comment);
        return ApiResponse.ok(null);
    }

    @GetMapping("/daily-report")
    public ApiResponse<List<DailyCashDTO>> dailyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate start = from != null ? from : date;
        LocalDate end = to != null ? to : date;
        return ApiResponse.ok(cashRegisterService.getReports(start, end));
    }
}
