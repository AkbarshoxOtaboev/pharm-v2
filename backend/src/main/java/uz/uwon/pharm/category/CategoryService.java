package uz.uwon.pharm.category;

import java.util.List;

public interface CategoryService {
    void save(CategoryDTO dto);
    List<CategoryResponse> findAllCategories();
    CategoryResponse findById(Long categoryId);
    void update(Long categoryId, CategoryDTO dto);
    void delete(Long categoryId);
}
