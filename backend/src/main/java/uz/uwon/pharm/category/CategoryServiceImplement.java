package uz.uwon.pharm.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.uwon.pharm.exceptions.BadRequestException;
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
        return findAllCategories(false);
    }

    @Override
    public List<CategoryResponse> findAllCategories(boolean includeInactive) {
        log.info("Finding categories, includeInactive={}", includeInactive);
        List<Category> categories = includeInactive
                ? repository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                : repository.findAllByStatusOrderByIdAsc(Status.ACTIVE);
        return categories.stream().map(this::mapToCategoryResponse).collect(Collectors.toList());
    }

    @Override
    public CategoryResponse findById(Long categoryId) {
        log.info("Finding category with id {}", categoryId);
        return repository.findByIdAndStatus(categoryId, Status.ACTIVE).map(this::mapToCategoryResponse).orElseThrow(()-> new NotFoundException("Category not found "+categoryId)) ;
    }

    @Override
    public void update(Long categoryId, CategoryDTO dto) {
        log.info("Updating category with id {}", categoryId);
        Category category = requireMutable(categoryId);
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
    }

    @Override
    public void delete(Long categoryId) {
        log.info("Deleting category with id {}", categoryId);
        Category category = requireMutable(categoryId);
        category.setStatus(Status.DELETED);
    }

    @Override
    public void block(Long categoryId) {
        log.info("Blocking category with id {}", categoryId);
        requireMutable(categoryId).setStatus(Status.BLOCKED);
    }

    @Override
    public void unblock(Long categoryId) {
        log.info("Unblocking category with id {}", categoryId);
        requireMutable(categoryId).setStatus(Status.ACTIVE);
    }

    private Category requireMutable(Long categoryId) {
        Category category = repository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found " + categoryId));
        if (category.getStatus() == Status.DELETED) {
            throw new BadRequestException("Deleted category cannot be changed");
        }
        return category;
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
