package uz.uwon.pharm.orderDeliveridinfo;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.uwon.pharm.common.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order comments")
public class OrderDeliveredInfoApiController {

    private final OrderDeliveredInfoService orderDeliveredInfoService;

    @GetMapping("/{orderId}/comments")
    public ApiResponse<List<OrderDeliveredInfoResponse>> list(@PathVariable Long orderId) {
        return ApiResponse.ok(orderDeliveredInfoService.getOrderDeliveredInfoResponses(orderId));
    }
}
