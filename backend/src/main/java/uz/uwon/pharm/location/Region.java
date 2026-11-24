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
@Table(name = "regions")
public class Region {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    private Status status;
}
