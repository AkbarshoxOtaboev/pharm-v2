package uz.uwon.pharm.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@AllArgsConstructor
public class OrderPageResponse<T> {
    private List<T> content;
    private int totalPages;
    private long totalElements;
    private int page;
}
