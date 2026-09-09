package uz.uwon.pharm.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.uwon.pharm.category.Category;
import uz.uwon.pharm.category.CategoryRepository;
import uz.uwon.pharm.category.CategoryResponse;
import uz.uwon.pharm.exceptions.NotFoundException;
import uz.uwon.pharm.storage.StorageService;
import uz.uwon.pharm.store.Store;
import uz.uwon.pharm.store.StoreRepository;
import uz.uwon.pharm.subCategory.SubCategory;
import uz.uwon.pharm.subCategory.SubCategoryRepository;
import uz.uwon.pharm.utils.Status;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class ProductServiceImplement implements ProductService {
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final ProductRepository repository;
    private final StoreRepository storeRepository;
    private final StorageService storageService;

    @Override
    public void save(ProductDTO dto) {
        log.info("Saving new product, {}", dto.getName());
        Product product = Product.builder()
                .name(dto.getName())
                .price(dto.getPrice())
                .priceCost(dto.getPriceCost())
                .category(categoryRepository.findById(dto.getCategoryId()).orElseThrow(() -> new NotFoundException("Category not found with id" + dto.getCategoryId())))
                .subCategory(subCategoryRepository.findById(dto.getSubCategoryId()).orElseThrow(() -> new NotFoundException("Subcategory not found with id" + dto.getSubCategoryId())))
                .sortNumber(dto.getSortNumber())
                .status(Status.ACTIVE)
                .description(dto.getDescription())
                .photo(storageService.store(dto.getPhoto()))
                .unitType(dto.getUnitType())
                .build();
        log.info("Saving product on store");
        Store store = Store.builder()
                .product(product)
                .quantity(BigDecimal.valueOf(0.0))
                .totalAmount(BigDecimal.valueOf(0.0))
                .status(Status.ACTIVE)
                .build();
        product.setStore(store);
        repository.save(product);
    }

    @Override
    public ProductResponse findById(Long productId) {
        log.info("Find product find id {} and map to product response", productId);
        return mapToProduct(repository.findProductByIdAndStatus(productId, Status.ACTIVE).orElseThrow(() -> new NotFoundException("Product not found with id " + productId)));
    }

    @Override
    public List<ProductResponse> findAllProducts() {
        log.info("Fetch all products");
        return repository.findAllProductByStatusOrderBySortNumberAsc(Status.ACTIVE).stream().map(this::mapToProduct).collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> findAllProductsByCategoryId(Long categoryId) {
        log.info("Find category by categoryId = {} for fetch products by category", categoryId);
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new NotFoundException("Category not found with id" + categoryId));
        log.info("Fetch products by category id {}", categoryId);
        return repository.findAllProductByCategoryAndStatusOrderBySortNumberAsc(category, Status.ACTIVE)
                .stream()
                .map(this::mapToProduct)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> findAllProductsBySubCategoryId(Long subCategoryId) {
        log.info("Find subcategory by subCategory = {} id for fetch product by subcategory", subCategoryId);
        SubCategory subCategory = subCategoryRepository.findById(subCategoryId).orElseThrow(() -> new NotFoundException("Subcategory not found with id" + subCategoryId));
        log.info("Fetch product by subcategory id");
        return repository.findAllProductsBySubCategoryAndStatusOrderBySortNumberAsc(subCategory, Status.ACTIVE)
                .stream()
                .map(this::mapToProduct)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> search(String productName) {
        return storeRepository.findByProduct_NameContainingIgnoreCaseAndQuantityGreaterThan(productName, BigDecimal.ZERO)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> findAllProductsGreaterThan() {
        return storeRepository.findAllByStatusAndQuantityGreaterThan(Status.ACTIVE, BigDecimal.ZERO)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void update(Long productId, ProductDTO dto) {
        log.info("Update product");
        Product product = repository.findProductByIdAndStatus(productId, Status.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Product not found with id" + productId));
        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setPriceCost(dto.getPriceCost());
        product.setCategory(categoryRepository.findById(dto.getCategoryId()).orElseThrow(() -> new NotFoundException("Category not found with categoryId" + dto.getCategoryId())));
        product.setSubCategory(subCategoryRepository.findById(dto.getSubCategoryId()).orElseThrow(() -> new NotFoundException("SubCategory not found with subCategoryId" + dto.getSubCategoryId())));
        product.setUnitType(dto.getUnitType());
        product.setSortNumber(dto.getSortNumber());
        product.setDescription(dto.getDescription());
        if(!dto.getPhoto().isEmpty() && dto.getPhoto() != null){
            storageService.delete(product.getPhoto());
            product.setPhoto(storageService.store(dto.getPhoto()));
        }
    }

    @Override
    public void delete(Long productId) {
        log.info("Delete product");
        Product product = repository.findProductByIdAndStatus(productId, Status.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Product not found with id" + productId));
        product.setStatus(Status.DELETED);
    }

    private ProductResponse mapToProduct(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getPriceCost(),
                mapToCategoryResponse(product.getCategory()),
                product.getSubCategory().getId(),
                product.getSortNumber(),
                product.getStatus(),
                product.getDescription(),
                product.getPhoto(),
                product.getUnitType()
        );
    }

    private CategoryResponse mapToCategoryResponse(Category category){
        return new CategoryResponse(
                category.getId(),
                category.getName()
        );
    }

    private ProductResponse mapToResponse(Store store) {
        Product p = store.getProduct();

        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getPriceCost(),
                store.getQuantity(),
                p.getUnitType()
        );
    }
}
