package uz.uwon.pharm.subCategory;

import java.util.List;

public interface SubCategoryService {
    void save(SubCategoryDTO dto);
    List<SubCategoryResponse> findAllByCategoryId(Long categoryId);
    Integer subCategoryCount(Long categoryId);
    SubCategoryResponse findById(Long subcategoryId);
    void update(Long subCategoryId, SubCategoryDTO dto);
    void delete(Long subCategoryId);
}
