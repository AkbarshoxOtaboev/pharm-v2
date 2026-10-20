package uz.uwon.pharm.cashRegister;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import uz.uwon.pharm.common.ApiResponse;
import uz.uwon.pharm.common.PageMeta;

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

    @PatchMapping("/courier/{courierId}/return-all")
    public ApiResponse<Map<String, BigDecimal>> returnAll(@PathVariable Long courierId,
                                                          @RequestBody(required = false) Map<String, String> body) {
        String comment = body != null ? body.get("comment") : null;
        return ApiResponse.ok(Map.of("total", cashRegisterService.returnAll(courierId, comment)));
    }

    @GetMapping("/summary")
    public ApiResponse<CashSummaryResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long courierId) {
        return ApiResponse.ok(cashRegisterService.summary(from, to, courierId));
    }

    @GetMapping("/page")
    public ApiResponse<List<CashRegisterResponse>> page(
            @RequestParam(required = false) Long courierId,
            @RequestParam(required = false) CashRegisterStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<CashRegisterResponse> result = cashRegisterService.page(courierId, status, from, to, page, size);
        return ApiResponse.ok(result.getContent(), PageMeta.from(result));
    }

    @GetMapping("/daily-report/page")
    public ApiResponse<List<DailyCashDTO>> dailyReportPage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<DailyCashDTO> result = cashRegisterService.dailyPage(from, to, page, size);
        return ApiResponse.ok(result.getContent(), PageMeta.from(result));
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
