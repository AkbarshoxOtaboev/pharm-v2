package uz.uwon.pharm.subCategory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.uwon.pharm.category.Category;
import uz.uwon.pharm.utils.Status;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {
    Integer countByCategoryAndStatus(Category category, Status status);

    Optional<SubCategory> findByIdAndStatus(Long id, Status status);
    List<SubCategory> findAllByCategoryAndStatusOrderByIdAsc(Category category, Status status);
}
