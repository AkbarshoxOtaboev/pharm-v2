package uz.uwon.pharm.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.uwon.pharm.utils.Status;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByIdAndStatus(Long id, Status status);
    List<Customer> findAllByStatus(Status status);
    boolean existsByPhone(String phone);

    Optional<Customer> findByPhone(String phone);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.status = 0")
    long countActiveCustomers();
}
