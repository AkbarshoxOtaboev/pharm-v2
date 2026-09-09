package uz.uwon.pharm.category;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uz.uwon.pharm.common.ApiResponse;
import uz.uwon.pharm.subCategory.SubCategoryDTO;
import uz.uwon.pharm.subCategory.SubCategoryResponse;
import uz.uwon.pharm.subCategory.SubCategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories")
public class CategoriesApiController {

    private final CategoryService categoryService;
    private final SubCategoryService subCategoryService;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> list() {
        return ApiResponse.ok(categoryService.findAllCategories());
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(categoryService.findById(id));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody CategoryDTO dto) {
        categoryService.save(dto);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody CategoryDTO dto) {
        categoryService.update(id, dto);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/subcategories")
    public ApiResponse<List<SubCategoryResponse>> subcategories(@PathVariable Long id) {
        return ApiResponse.ok(subCategoryService.findAllByCategoryId(id));
    }

    @PostMapping("/{id}/subcategories")
    public ApiResponse<Void> createSubcategory(@PathVariable Long id, @RequestBody SubCategoryDTO dto) {
        dto.setCategoryId(id);
        subCategoryService.save(dto);
        return ApiResponse.ok(null);
    }
}
