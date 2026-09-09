package uz.uwon.pharm.address;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.uwon.pharm.customer.Customer;
import uz.uwon.pharm.customer.CustomerRepository;
import uz.uwon.pharm.exceptions.NotFoundException;
import uz.uwon.pharm.utils.Status;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class AddressServiceImplement implements AddressService {
    private final AddressRepository repository;
    private final CustomerRepository customerRepository;

    @Override
    public void save(Long customerId, AddressDTO dto) {
        log.info("Fetch customer for saving address: customerId = {}", customerId);
        Customer customer = customerRepository.findByIdAndStatus(customerId, Status.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Customer not found with id" + customerId));
        log.info("Convert address dto");
        Address address = Address.builder()
                .fullAddress(dto.getFullAddress())
                .home(dto.getHome())
                .entrance(dto.getEntrance())
                .floor(dto.getFloor())
                .orientation(dto.getOrientation())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .status(Status.ACTIVE)
                .customer(customer)
                .build();
        log.info("Saving address");
        repository.save(address);

    }

    @Override
    public AddressResponse findById(Long addressId) {
        log.info("Find address by id {}", addressId);
        return mapToAddressResponse(repository.findByIdAndStatus(addressId, Status.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Address not found with id" + addressId)));
    }

    @Override
    public List<AddressResponse> findAll(Long customerId) {
        log.info("Fetch all active addresses");
        Customer customer = customerRepository.findById(customerId).orElseThrow(()->new NotFoundException("Client not found with id =" + customerId ));
        return repository.findAllByCustomerAndStatus(customer,Status.ACTIVE)
                .stream()
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void update(Long addressId, AddressDTO dto) {
        log.info("Update address with id {}", addressId);
        Address address = repository.findByIdAndStatus(addressId, Status.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Address not found with id" + addressId));
        address.setFullAddress(dto.getFullAddress());
        address.setHome(dto.getHome());
        address.setEntrance(dto.getEntrance());
        address.setFloor(dto.getFloor());
        address.setOrientation(dto.getOrientation());
        address.setLatitude(dto.getLatitude());
        address.setLongitude(dto.getLongitude());
    }

    @Override
    public void delete(Long addressId) {
        log.info("Delete address with id {}",addressId);
        Address address = repository.findByIdAndStatus(addressId, Status.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Address not found with id" + addressId));
        address.setStatus(Status.DELETED);

    }

    private AddressResponse mapToAddressResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getFullAddress(),
                address.getHome(),
                address.getEntrance(),
                address.getApartment(),
                address.getFloor(),
                address.getOrientation(),
                address.getLatitude(),
                address.getLongitude(),
                address.getStatus(),
                address.getCreatedAt()
        );
    }
}
