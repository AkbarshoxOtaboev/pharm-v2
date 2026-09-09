package uz.uwon.pharm.address;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.uwon.pharm.customer.Customer;
import uz.uwon.pharm.utils.Status;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    Optional<Address> findByIdAndStatus(Long id, Status status);
    List<Address> findAllByCustomerAndStatus(Customer customer,Status status);
}
