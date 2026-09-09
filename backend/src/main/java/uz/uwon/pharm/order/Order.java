package uz.uwon.pharm.order;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import uz.uwon.pharm.customer.Customer;
import uz.uwon.pharm.orderDeliveridinfo.OrderDeliveredInfo;
import uz.uwon.pharm.orderitem.OrderItem;
import uz.uwon.pharm.users.User;
import uz.uwon.pharm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = TableName.ORDERS)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String orderNumber;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @CreationTimestamp(source = SourceType.DB)
    private LocalDateTime createdAt;

    

    @UpdateTimestamp(source = SourceType.DB)
    private LocalDateTime updatedAt;
    @OneToMany(mappedBy = "order", cascade =  CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;
    private BigDecimal totalSum;
    private String address;
    private String home;
    private String entrance;
    private String apartment;
    private String floor;
    private String orientations;
    private PaymentType paymentType;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDeliveredInfo> infos;

    @PrePersist
    public void generateOrderNumber(){
        if(orderNumber == null){
            orderNumber = "ORD - "+ UUID.randomUUID().toString().substring(0,4).toUpperCase();
        }
        if(orderStatus == null){
            orderStatus = OrderStatus.NEW;
        }
    }

}
