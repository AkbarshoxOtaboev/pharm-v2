package uz.uwon.pharm.customer;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uz.uwon.pharm.address.AddressDTO;
import uz.uwon.pharm.address.AddressResponse;
import uz.uwon.pharm.address.AddressService;
import uz.uwon.pharm.common.ApiResponse;

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
