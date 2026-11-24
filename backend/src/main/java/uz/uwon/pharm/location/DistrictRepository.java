package uz.uwon.pharm.location;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.uwon.pharm.utils.Status;

import java.util.List;
import java.util.Optional;

public interface DistrictRepository extends JpaRepository<District, Long> {
    List<District> findAllByStatusOrderByNameAsc(Status status);
    List<District> findAllByRegionIdAndStatusOrderByNameAsc(Long regionId, Status status);
    Optional<District> findByIdAndStatus(Long id, Status status);
}
