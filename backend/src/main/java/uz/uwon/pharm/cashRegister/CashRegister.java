package uz.uwon.pharm.cashRegister;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import uz.uwon.pharm.users.User;
import uz.uwon.pharm.utils.Status;
import uz.uwon.pharm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = TableName.CASH_REGISTER)
public class CashRegister {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    private User courier;
    private Long orderId;
    private BigDecimal orderTotalSum;
    @Enumerated(EnumType.STRING)
    private CashRegisterStatus registerStatus;
    private LocalDateTime dateTime;
    private String comment;
    @CreationTimestamp(source = SourceType.DB)
    private LocalDateTime createdAt;
    @UpdateTimestamp(source = SourceType.DB)
    private LocalDateTime updateAt;
    @Enumerated(EnumType.STRING)
    private Status status;
}
