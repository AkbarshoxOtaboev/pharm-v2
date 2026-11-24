package uz.uwon.pharm.location;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uz.uwon.pharm.exceptions.BadRequestException;
import uz.uwon.pharm.exceptions.NotFoundException;
import uz.uwon.pharm.utils.Status;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class LocationService {

    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;

    public List<LocationDtos.RegionResponse> regions() {
        return regionRepository.findAllByStatusOrderByNameAsc(Status.ACTIVE).stream()
                .map(r -> new LocationDtos.RegionResponse(r.getId(), r.getName()))
                .toList();
    }

    public LocationDtos.RegionResponse createRegion(String name) {
        Region region = Region.builder().name(requireName(name)).status(Status.ACTIVE).build();
        region = regionRepository.save(region);
        return new LocationDtos.RegionResponse(region.getId(), region.getName());
    }

    public void deleteRegion(Long id) {
        Region region = regionRepository.findByIdAndStatus(id, Status.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Region not found"));
        region.setStatus(Status.DELETED);
        for (District district : districtRepository.findAllByRegionIdAndStatusOrderByNameAsc(id, Status.ACTIVE)) {
            district.setStatus(Status.DELETED);
        }
    }

    public List<LocationDtos.DistrictResponse> districts(Long regionId) {
        List<District> rows = regionId == null
                ? districtRepository.findAllByStatusOrderByNameAsc(Status.ACTIVE)
                : districtRepository.findAllByRegionIdAndStatusOrderByNameAsc(regionId, Status.ACTIVE);
        return rows.stream().map(this::toDistrict).toList();
    }

    public LocationDtos.DistrictResponse createDistrict(String name, Long regionId) {
        if (regionId == null) throw new BadRequestException("Region is required");
        Region region = regionRepository.findByIdAndStatus(regionId, Status.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Region not found"));
        District district = districtRepository.save(District.builder()
                .name(requireName(name))
                .region(region)
                .status(Status.ACTIVE)
                .build());
        return toDistrict(district);
    }

    public void deleteDistrict(Long id) {
        District district = districtRepository.findByIdAndStatus(id, Status.ACTIVE)
                .orElseThrow(() -> new NotFoundException("District not found"));
        district.setStatus(Status.DELETED);
    }

    public Region requireRegion(Long id) {
        return regionRepository.findByIdAndStatus(id, Status.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Region not found"));
    }

    public District requireDistrict(Long id) {
        return districtRepository.findByIdAndStatus(id, Status.ACTIVE)
                .orElseThrow(() -> new NotFoundException("District not found"));
    }

    private LocationDtos.DistrictResponse toDistrict(District district) {
        return new LocationDtos.DistrictResponse(
                district.getId(),
                district.getName(),
                district.getRegion().getId(),
                district.getRegion().getName()
        );
    }

    private String requireName(String name) {
        if (!StringUtils.hasText(name)) throw new BadRequestException("Name is required");
        return name.trim();
    }
}
