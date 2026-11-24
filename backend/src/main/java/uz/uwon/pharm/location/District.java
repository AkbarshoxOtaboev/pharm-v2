package uz.uwon.pharm.location;

import jakarta.persistence.*;
import lombok.*;
import uz.uwon.pharm.utils.Status;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "districts")
public class District {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;
    @Enumerated(EnumType.STRING)
    private Status status;
}
