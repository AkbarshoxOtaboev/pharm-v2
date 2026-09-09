package uz.uwon.pharm.customer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.uwon.pharm.exceptions.NotFoundException;
import uz.uwon.pharm.utils.Status;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class CustomerServiceImplement implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    public void save(CustomerDTO customerDTO) {
        log.info("Saving customer");
        Customer customer = Customer.builder()
                .fullName(customerDTO.getFullName())
                .phone(customerDTO.getPhone().replaceAll("[^0-9]", ""))
                .birthDate(customerDTO.getBirthDate())
                .description(customerDTO.getDescription())
                .gender(customerDTO.getGender())
                .type(customerDTO.getType())
                .status(Status.ACTIVE)
                .build();
        customerRepository.save(customer);
    }

    @Override
    public CustomerResponse findById(Long id) {
        log.info("Find customer by id");
        return mapToCustomer(customerRepository.findByIdAndStatus(id, Status.ACTIVE).orElseThrow(()->new NotFoundException("Customer with id " + id + " not found!")));
    }

    @Override
    public List<CustomerResponse> findAll() {
        log.info("Fetch all customers");
        return customerRepository.findAll().stream().map(this::mapToCustomer).collect(Collectors.toList());
    }

    @Override
    public void update(Long id, CustomerDTO customerDTO) {
        log.info("Updating customer with id {}", id);
        Customer customer = customerRepository.findByIdAndStatus(id, Status.ACTIVE).orElseThrow(()->new NotFoundException("Customer with id " + id + " not found!"));
        customer.setFullName(customerDTO.getFullName());
        customer.setPhone(customerDTO.getPhone());
        customer.setBirthDate(customerDTO.getBirthDate());
        customer.setDescription(customerDTO.getDescription());
        customer.setType(customerDTO.getType());
    }

    @Override
    public void delete(Long id) {
        log.info("Delete customer with id {}", id);
        Customer customer = customerRepository.findByIdAndStatus(id, Status.ACTIVE).orElseThrow(()->new NotFoundException("Customer with id " + id + " not found!"));
        customer.setStatus(Status.DELETED);
    }

    @Override
    public void restore(Long id) {
        log.info("Restore customer with id {}", id);
        Customer customer = customerRepository.findById(id).orElseThrow(()->new NotFoundException("Customer with id " + id + " not found!"));
        customer.setStatus(Status.ACTIVE);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return customerRepository.existsByPhone(phone.replaceAll("[^0-9]", ""));
    }

    @Override
    public CustomerResponse search(String phone) {
        return customerRepository.findByPhone(phone).map(this::mapToCustomer).orElse(null);
    }

    private CustomerResponse mapToCustomer(Customer customer){
        return new CustomerResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getBirthDate(),
                customer.getDescription(),
                customer.getGender(),
                customer.getType(),
                customer.getStatus(),
                customer.getCreatedAt()
        );
    }
}
