package uz.uwon.pharm.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.uwon.pharm.utils.Status;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByStatusOrderByIdAsc(Status status);
    Optional<Category> findByIdAndStatus(Long id, Status status);
}
