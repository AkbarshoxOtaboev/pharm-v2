package uz.uwon.pharm.subCategory;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uz.uwon.pharm.common.ApiResponse;

@RestController
@RequestMapping("/api/v1/subcategories")
@RequiredArgsConstructor
@Tag(name = "SubCategories")
public class SubCategoriesApiController {

    private final SubCategoryService subCategoryService;

    @GetMapping("/{id}")
    public ApiResponse<SubCategoryResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(subCategoryService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody SubCategoryDTO dto) {
        subCategoryService.update(id, dto);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        subCategoryService.delete(id);
        return ApiResponse.ok(null);
    }
}
