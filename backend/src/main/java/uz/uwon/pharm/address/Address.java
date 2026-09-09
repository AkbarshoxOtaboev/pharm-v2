package uz.uwon.pharm.address;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import uz.uwon.pharm.customer.Customer;
import uz.uwon.pharm.utils.Status;
import uz.uwon.pharm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = TableName.ADDRESSES)
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullAddress;
    private String home;
    private String entrance;
    private String apartment;
    private String floor;
    private String orientation;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Status status;
    @CreationTimestamp(source = SourceType.DB)
    private LocalDate createdAt;
    @UpdateTimestamp(source = SourceType.DB)
    private LocalDate updatedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

}
