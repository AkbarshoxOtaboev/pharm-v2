package uz.uwon.pharm.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.uwon.pharm.exceptions.NotFoundException;
import uz.uwon.pharm.subCategory.SubCategoryRepository;
import uz.uwon.pharm.subCategory.SubCategoryService;
import uz.uwon.pharm.utils.Status;

import java.util.List;
import java.util.stream.Collectors;
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CategoryServiceImplement implements CategoryService {

    private final CategoryRepository repository;
    private final SubCategoryService subCategoryService;

    @Override
    public void save(CategoryDTO dto) {
        log.info("Saving category with name {}", dto.getName());
        Category category = Category.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .status(Status.ACTIVE)
                .build();
        repository.save(category);
    }

    @Override
    public List<CategoryResponse> findAllCategories() {
        log.info("Finding all categories");
        return repository.findAllByStatusOrderByIdAsc(Status.ACTIVE).stream().map(this::mapToCategoryResponse).collect(Collectors.toList());
    }

    @Override
    public CategoryResponse findById(Long categoryId) {
        log.info("Finding category with id {}", categoryId);
        return repository.findByIdAndStatus(categoryId, Status.ACTIVE).map(this::mapToCategoryResponse).orElseThrow(()-> new NotFoundException("Category not found "+categoryId)) ;
    }

    @Override
    public void update(Long categoryId, CategoryDTO dto) {
        log.info("Updating category with id {}", categoryId);
        Category category = repository.findByIdAndStatus(categoryId, Status.ACTIVE).orElseThrow(()-> new NotFoundException("Category not found "+categoryId));
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
    }

    @Override
    public void delete(Long categoryId) {
        log.info("Deleting category with id {}", categoryId);
        Category category = repository.findByIdAndStatus(categoryId,Status.ACTIVE).orElseThrow(()-> new NotFoundException("Category not found "+categoryId));
        category.setStatus(Status.DELETED);
    }

    private CategoryResponse mapToCategoryResponse(Category category) {
        Integer count = subCategoryService.subCategoryCount(category.getId());
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                count,
                category.getStatus()
        );
    }
}
