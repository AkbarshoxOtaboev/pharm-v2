package uz.uwon.pharm.product;

import java.util.List;

public interface ProductService {
    void save(ProductDTO dto);
    ProductResponse findById(Long productId);
    List<ProductResponse> findAllProducts();
    List<ProductResponse> findAllProductsByCategoryId(Long categoryId);
    List<ProductResponse> findAllProductsBySubCategoryId(Long subCategoryId);
    List<ProductResponse> search(String productName);
    List<ProductResponse> findAllProductsGreaterThan();
    void update(Long productId, ProductDTO dto);
    void delete(Long productId);
}
