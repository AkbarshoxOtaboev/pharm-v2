package uz.uwon.pharm.product;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import uz.uwon.pharm.common.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products")
public class ProductsApiController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<List<ProductResponse>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subCategoryId) {
        if (subCategoryId != null) {
            return ApiResponse.ok(productService.findAllProductsBySubCategoryId(subCategoryId));
        }
        if (categoryId != null) {
            return ApiResponse.ok(productService.findAllProductsByCategoryId(categoryId));
        }
        return ApiResponse.ok(productService.findAllProducts());
    }

    @GetMapping("/in-stock")
    public ApiResponse<List<ProductResponse>> inStock() {
        return ApiResponse.ok(productService.findAllProductsGreaterThan());
    }

    @GetMapping("/search")
    public ApiResponse<List<ProductResponse>> search(@RequestParam String q) {
        return ApiResponse.ok(productService.search(q));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(productService.findById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> create(@ModelAttribute ProductDTO dto) {
        productService.save(dto);
        return ApiResponse.ok(null);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> update(@PathVariable Long id, @ModelAttribute ProductDTO dto) {
        productService.update(id, dto);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.ok(null);
    }
}
