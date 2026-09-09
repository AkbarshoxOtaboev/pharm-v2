package uz.uwon.pharm.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.uwon.pharm.category.Category;
import uz.uwon.pharm.subCategory.SubCategory;
import uz.uwon.pharm.utils.Status;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findProductByIdAndStatus(Long id, Status status);
    List<Product> findAllProductByStatusOrderBySortNumberAsc(Status status);
    List<Product> findAllProductByCategoryAndStatusOrderBySortNumberAsc(Category category, Status status);
    List<Product> findAllProductsBySubCategoryAndStatusOrderBySortNumberAsc(SubCategory subCategory, Status status);
}
