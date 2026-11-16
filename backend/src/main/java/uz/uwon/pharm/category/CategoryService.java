package uz.uwon.pharm.category;

import java.util.List;

public interface CategoryService {
    void save(CategoryDTO dto);
    List<CategoryResponse> findAllCategories();
    List<CategoryResponse> findAllCategories(boolean includeInactive);
    CategoryResponse findById(Long categoryId);
    void update(Long categoryId, CategoryDTO dto);
    void delete(Long categoryId);
    void block(Long categoryId);
    void unblock(Long categoryId);
}
