package uz.uwon.pharm.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
    private String name;
    private BigDecimal price;
    private BigDecimal priceCost;
    private Long categoryId;
    private Long subCategoryId;
    private Integer sortNumber;
    private String description;
    private MultipartFile photo;
    private UnitType unitType;
}
