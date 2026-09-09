package uz.uwon.pharm.store;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import uz.uwon.pharm.common.ApiResponse;
import uz.uwon.pharm.storeHistory.StoreHistoryResponse;
import uz.uwon.pharm.storeHistory.StoreHistoryService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/store")
@RequiredArgsConstructor
@Tag(name = "Store")
public class StoreApiController {

    private final StoreService storeService;
    private final StoreHistoryService storeHistoryService;

    @GetMapping
    public ApiResponse<List<StoreResponse>> list() {
        return ApiResponse.ok(storeService.findAll());
    }

    @GetMapping("/history")
    public ApiResponse<List<StoreHistoryResponse>> history(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from != null || to != null) {
            return ApiResponse.ok(storeHistoryService.getFiltered(from, to));
        }
        return ApiResponse.ok(storeHistoryService.findAll());
    }

    @GetMapping("/statistics")
    public ApiResponse<StoreStatisticsResponse> statistics() {
        return ApiResponse.ok(storeService.getStoreStatistics());
    }

    @GetMapping("/{id}")
    public ApiResponse<StoreResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(storeService.findById(id));
    }

    @PostMapping("/arrival")
    public ApiResponse<Void> arrival(@RequestParam Long storeId, @RequestBody StoreDTO dto) {
        storeService.saveArrivalProduct(storeId, dto);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}/arrival")
    public ApiResponse<Void> updateArrival(@PathVariable Long id, @RequestBody StoreDTO dto) {
        storeService.updateArrivalProduct(id, dto);
        return ApiResponse.ok(null);
    }
}
