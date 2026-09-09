package uz.uwon.pharm.address;

import java.util.List;

public interface AddressService {
    void save(Long customerId, AddressDTO dto);
    AddressResponse findById(Long addressId);
    List<AddressResponse> findAll(Long customerId);
    void update(Long addressId, AddressDTO dto);
    void delete(Long addressId);
}
