package uz.uwon.pharm.storeHistory;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import uz.uwon.pharm.utils.Status;
import uz.uwon.pharm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = TableName.STORE_HISTORIES)
public class StoreHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal priceCost;
    private BigDecimal totalAmount;
    private LocalDate dateOfArrival;
    private String comment;
    @Enumerated(EnumType.STRING)
    private Status status;
    @CreationTimestamp(source = SourceType.DB)
    private LocalDateTime createdAt;
    @UpdateTimestamp(source = SourceType.DB)
    private LocalDateTime updatedAt;
    private Long storeId;
}
