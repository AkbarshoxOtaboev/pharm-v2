package uz.uwon.pharm.customer;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import uz.uwon.pharm.address.AddressDTO;
import uz.uwon.pharm.address.AddressResponse;
import uz.uwon.pharm.address.AddressService;
import org.springframework.data.domain.Page;
import uz.uwon.pharm.common.ApiResponse;
import uz.uwon.pharm.common.PageMeta;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers")
public class CustomersApiController {

    private final CustomerService customerService;
    private final AddressService addressService;

    @GetMapping
    public ApiResponse<List<CustomerResponse>> list() {
        return ApiResponse.ok(customerService.findAll());
    }

    @GetMapping("/page")
    public ApiResponse<List<CustomerStatsResponse>> page(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<CustomerStatsResponse> result = customerService.pageWithStats(q, page, size);
        return ApiResponse.ok(result.getContent(), PageMeta.from(result));
    }

    @GetMapping("/sales-summary")
    public ApiResponse<CustomerSalesSummaryResponse> salesSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(customerService.salesSummary(from, to));
    }

    @GetMapping("/sales-page")
    public ApiResponse<List<CustomerSalesRowResponse>> salesPage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ORDERED") String segment,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<CustomerSalesRowResponse> result = customerService.salesPage(from, to, segment, page, size);
        return ApiResponse.ok(result.getContent(), PageMeta.from(result));
    }

    @GetMapping("/search")
    public ApiResponse<CustomerResponse> search(@RequestParam String phone) {
        return ApiResponse.ok(customerService.search(phone));
    }

    @GetMapping("/check-phone")
    public ApiResponse<Map<String, Boolean>> checkPhone(@RequestParam String phone) {
        return ApiResponse.ok(Map.of("exists", customerService.existsByPhone(phone)));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(customerService.findById(id));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody CustomerDTO dto) {
        customerService.save(dto);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody CustomerDTO dto) {
        customerService.update(id, dto);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{id}/restore")
    public ApiResponse<Void> restore(@PathVariable Long id) {
        customerService.restore(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{customerId}/addresses")
    public ApiResponse<List<AddressResponse>> addresses(@PathVariable Long customerId) {
        return ApiResponse.ok(addressService.findAll(customerId));
    }

    @PostMapping("/{customerId}/addresses")
    public ApiResponse<Void> addAddress(@PathVariable Long customerId, @RequestBody AddressDTO dto) {
        addressService.save(customerId, dto);
        return ApiResponse.ok(null);
    }
}
