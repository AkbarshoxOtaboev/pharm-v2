package uz.uwon.pharm.location;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uz.uwon.pharm.common.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationsApiController {

    private final LocationService locationService;

    @GetMapping("/regions")
    public ApiResponse<List<LocationDtos.RegionResponse>> regions() {
        return ApiResponse.ok(locationService.regions());
    }

    @PostMapping("/regions")
    public ApiResponse<LocationDtos.RegionResponse> createRegion(@RequestBody LocationDtos.NameRequest body) {
        return ApiResponse.ok(locationService.createRegion(body.name()));
    }

    @DeleteMapping("/regions/{id}")
    public ApiResponse<Void> deleteRegion(@PathVariable Long id) {
        locationService.deleteRegion(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/districts")
    public ApiResponse<List<LocationDtos.DistrictResponse>> districts(@RequestParam(required = false) Long regionId) {
        return ApiResponse.ok(locationService.districts(regionId));
    }

    @PostMapping("/districts")
    public ApiResponse<LocationDtos.DistrictResponse> createDistrict(@RequestBody LocationDtos.NameRequest body) {
        return ApiResponse.ok(locationService.createDistrict(body.name(), body.parentId()));
    }

    @DeleteMapping("/districts/{id}")
    public ApiResponse<Void> deleteDistrict(@PathVariable Long id) {
        locationService.deleteDistrict(id);
        return ApiResponse.ok(null);
    }
}
