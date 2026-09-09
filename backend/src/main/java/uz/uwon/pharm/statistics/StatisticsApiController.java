package uz.uwon.pharm.statistics;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.uwon.pharm.common.ApiResponse;
import uz.uwon.pharm.order.DailyStatDTO;
import uz.uwon.pharm.order.OrderService;
import uz.uwon.pharm.order.OrderStatResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@Tag(name = "Statistics")
public class StatisticsApiController {

    private final OrderService orderService;

    @GetMapping("/dashboard")
    public ApiResponse<OrderStatResponse> dashboard() {
        return ApiResponse.ok(orderService.orderStatResponse());
    }

    @GetMapping("/daily")
    public ApiResponse<List<DailyStatDTO>> daily() {
        return ApiResponse.ok(orderService.getDailyStats());
    }

    @GetMapping("/monthly-sales")
    public ApiResponse<List<BigDecimal>> monthlySales(
            @RequestParam(required = false) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        return ApiResponse.ok(orderService.getMonthlySales(y));
    }
}
