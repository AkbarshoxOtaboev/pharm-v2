package uz.uwon.pharm.customer;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import uz.uwon.pharm.address.Address;
import uz.uwon.pharm.order.Order;
import uz.uwon.pharm.utils.Status;
import uz.uwon.pharm.utils.TableName;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = TableName.CUSTOMERS)
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    @Column(unique = true)
    private String phone;
    private LocalDate birthDate;
    private String description;
    private Gender gender;
    private CustomerType type;
    private Status status;
    @CreationTimestamp(source = SourceType.DB)
    private LocalDate createdAt;
    @CreationTimestamp(source = SourceType.DB)
    private LocalDate updatedAt;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders;

    @PrePersist
    public void setDefaultStatus() {
        if (status == null) {
            status = Status.ACTIVE;
        }
        if(gender == null){
            gender = Gender.NOT_SELECTED;
        }
    }

}
