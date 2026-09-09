package uz.uwon.pharm.category;

import lombok.Getter;
import lombok.Setter;
import uz.uwon.pharm.utils.Status;

@Getter
@Setter
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;
    private Integer subcategoryCount;
    private Status status;

    public CategoryResponse(Long id, String name, String description, Integer subcategoryCount, Status status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.subcategoryCount = subcategoryCount;
        this.status = status;
    }

    public CategoryResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}


