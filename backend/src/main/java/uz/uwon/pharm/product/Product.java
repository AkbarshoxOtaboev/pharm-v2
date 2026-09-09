package uz.uwon.pharm.product;

import jakarta.persistence.*;
import jdk.dynalink.linker.LinkerServices;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import uz.uwon.pharm.category.Category;
import uz.uwon.pharm.courierStock.CourierStock;
import uz.uwon.pharm.order.Order;
import uz.uwon.pharm.orderitem.OrderItem;
import uz.uwon.pharm.store.Store;
import uz.uwon.pharm.subCategory.SubCategory;
import uz.uwon.pharm.utils.Status;
import uz.uwon.pharm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = TableName.PRODUCTS)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private BigDecimal price;
    private BigDecimal priceCost;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategory;
    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL)
    private Store store;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    List<OrderItem> orderItems;

    private Integer sortNumber;
    @Enumerated(EnumType.STRING)
    private Status status;
    private String description;
    private String photo;
    @Enumerated(EnumType.STRING)
    private UnitType unitType;
    @CreationTimestamp(source = SourceType.DB)
    private LocalDate createdAt;
    @CreationTimestamp(source = SourceType.DB)
    private LocalDate updatedAt;

}
