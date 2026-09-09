package uz.uwon.pharm.address;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uz.uwon.pharm.common.ApiResponse;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses")
public class AddressesApiController {

    private final AddressService addressService;

    @GetMapping("/{id}")
    public ApiResponse<AddressResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(addressService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody AddressDTO dto) {
        addressService.update(id, dto);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return ApiResponse.ok(null);
    }
}
