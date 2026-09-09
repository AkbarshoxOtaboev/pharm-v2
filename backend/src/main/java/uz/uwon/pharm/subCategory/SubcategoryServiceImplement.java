package uz.uwon.pharm.subCategory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.uwon.pharm.category.Category;
import uz.uwon.pharm.category.CategoryRepository;
import uz.uwon.pharm.exceptions.NotFoundException;
import uz.uwon.pharm.utils.Status;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class SubcategoryServiceImplement implements SubCategoryService {
    private final SubCategoryRepository repository;
    private final CategoryRepository categoryRepository;
    @Override
    public void save(SubCategoryDTO dto) {
        log.info("Saving subcategory {}", dto.getName());
        SubCategory subCategory = SubCategory.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .category(categoryRepository.findById(dto.getCategoryId()).orElseThrow(() -> new NotFoundException("Category not found with id = " + dto.getCategoryId())))
                .status(Status.ACTIVE)
                .build();
        repository.save(subCategory);
    }

    @Override
    public List<SubCategoryResponse> findAllByCategoryId(Long categoryId) {
        log.info("Finding subcategories by category id {}", categoryId);
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new NotFoundException("Category not found with id = " + categoryId));
        return repository.findAllByCategoryAndStatusOrderByIdAsc(category, Status.ACTIVE).stream().map(this::mapToSubCategory).collect(Collectors.toList());
    }

    @Override
    public Integer subCategoryCount(Long categoryId) {
        log.info("Finding subcategories count by category id {}", categoryId);
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new NotFoundException("Category not found with id"));
        return repository.countByCategoryAndStatus(category, Status.ACTIVE);
    }

    @Override
    public SubCategoryResponse findById(Long subcategoryId) {
        log.info("Finding subcategories by subcategory id {}", subcategoryId);
        return mapToSubCategory(repository.findByIdAndStatus(subcategoryId, Status.ACTIVE).orElseThrow(()-> new NotFoundException("Sub category not found with id = " + subcategoryId)));
    }

    @Override
    public void update(Long subCategoryId, SubCategoryDTO dto) {
        log.info("Updating subcategories by subcategory id {}", subCategoryId);
        SubCategory subCategory = repository.findByIdAndStatus(subCategoryId, Status.ACTIVE).orElseThrow(() -> new NotFoundException("Subcategory not found with id = " + subCategoryId));
        subCategory.setName(dto.getName());
        subCategory.setDescription(dto.getDescription());
    }

    @Override
    public void delete(Long subCategoryId) {
        log.info("Deleting subcategories by subcategory id {}", subCategoryId);
        SubCategory subCategory = repository.findByIdAndStatus(subCategoryId, Status.ACTIVE).orElseThrow(() -> new NotFoundException("Subcategory not found with id = " + subCategoryId));
        subCategory.setStatus(Status.DELETED);
    }

    private SubCategoryResponse mapToSubCategory(SubCategory subCategory) {
        return new SubCategoryResponse(
                subCategory.getId(),
                subCategory.getName(),
                subCategory.getDescription(),
                subCategory.getStatus()

        );
    }
}
