package uz.uwon.pharm.location;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.uwon.pharm.utils.Status;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {
    List<Region> findAllByStatusOrderByNameAsc(Status status);
    Optional<Region> findByIdAndStatus(Long id, Status status);
}
