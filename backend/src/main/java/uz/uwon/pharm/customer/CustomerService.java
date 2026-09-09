package uz.uwon.pharm.customer;

import java.util.List;

public interface CustomerService {
    void save(CustomerDTO customerDTO);
    CustomerResponse findById(Long id);
    List<CustomerResponse> findAll();
    void update(Long id,CustomerDTO customerDTO);
    void delete(Long id);
    void restore(Long id);
    boolean existsByPhone(String phone);
    CustomerResponse search(String phone);
}
