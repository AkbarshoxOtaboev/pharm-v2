package uz.uwon.pharm.subCategory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import uz.uwon.pharm.utils.Status;

@Getter
@Setter
@AllArgsConstructor
public class SubCategoryResponse {
    private Long id;
    private String name;
    private String description;
    private Status status;
}
