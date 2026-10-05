package uz.uwon.pharm.customer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.uwon.pharm.exceptions.NotFoundException;
import uz.uwon.pharm.order.OrderRepository;
import uz.uwon.pharm.utils.PhoneFormatter;
import uz.uwon.pharm.utils.Status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class CustomerServiceImplement implements CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    @Override
    public void save(CustomerDTO customerDTO) {
        log.info("Saving customer");
        Customer customer = Customer.builder()
                .fullName(customerDTO.getFullName())
                .phone(PhoneFormatter.normalize(customerDTO.getPhone()))
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
        customer.setPhone(PhoneFormatter.normalize(customerDTO.getPhone()));
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
        return customerRepository.existsByPhone(PhoneFormatter.normalize(phone));
    }

    @Override
    public CustomerResponse search(String phone) {
        return customerRepository.findByPhoneAndStatus(PhoneFormatter.normalize(phone), Status.ACTIVE)
                .map(customer -> {
                    CustomerResponse response = mapToCustomer(customer);
                    orderRepository.findFirstByCustomerIdOrderByIdDesc(customer.getId())
                            .ifPresent(order -> response.setLastAddress(order.getAddress()));
                    return response;
                })
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerStatsResponse> pageWithStats(String query, int page, int size) {
        String q = query == null ? "" : query.trim().toLowerCase();
        String digits = q.replaceAll("[^0-9]", "");
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 250));
        return customerRepository.searchWithStats(
                        q.isEmpty(),
                        "%" + q + "%",
                        digits.isEmpty() ? "#" : "%" + digits + "%",
                        pageable)
                .map(this::mapToStats);
    }

    private CustomerStatsResponse mapToStats(CustomerStatsView v) {
        long count = v.getOrderCount() == null ? 0 : v.getOrderCount();
        BigDecimal sum = v.getOrderSum() == null ? BigDecimal.ZERO : v.getOrderSum();
        BigDecimal avg = count == 0 ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP);
        return new CustomerStatsResponse(
                v.getId(),
                v.getFullName(),
                v.getPhone(),
                v.getBirthDate(),
                v.getDescription(),
                v.getGender(),
                v.getType(),
                v.getStatus(),
                v.getCreatedAt(),
                count,
                sum,
                avg
        );
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
                customer.getCreatedAt(),
                null
        );
    }
}
