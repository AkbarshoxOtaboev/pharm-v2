package uz.uwon.pharm.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import uz.uwon.pharm.category.CategoryResponse;
import uz.uwon.pharm.utils.Status;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private BigDecimal priceCost;
    private CategoryResponse categoryResponse;
    private Long subCategoryId;
    private Integer sortNumber;
    private Status status;
    private String description;
    private String photo;
    private UnitType unitType;
    private BigDecimal storeQuantity;

    public String getUnitTypeLabel() {
        return unitType.getLabel();
    }

    public ProductResponse(Long id, String name, BigDecimal price, BigDecimal priceCost, CategoryResponse categoryResponse, Long subCategoryId, Integer sortNumber, Status status, String description, String photo, UnitType unitType) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.priceCost = priceCost;
        this.categoryResponse = categoryResponse;
        this.subCategoryId = subCategoryId;
        this.sortNumber = sortNumber;
        this.status = status;
        this.description = description;
        this.photo = photo;
        this.unitType = unitType;
    }

    public ProductResponse(Long id, String name, BigDecimal priceCost, BigDecimal storeQuantity, UnitType unitType) {
        this.id = id;
        this.name = name;
        this.priceCost = priceCost;
        this.storeQuantity = storeQuantity;
        this.unitType = unitType;
    }
}
