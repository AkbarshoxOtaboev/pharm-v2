package uz.uwon.pharm.orderDeliveridinfo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class OrderDeliveredInfoResponse {
    private String image;
    private String comment;
    private LocalDateTime createdAt;
}
