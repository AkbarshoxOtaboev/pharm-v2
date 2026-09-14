package uz.uwon.pharm.stockflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uz.uwon.pharm.courierStock.CourierStock;
import uz.uwon.pharm.courierStock.CourierStockDTO;
import uz.uwon.pharm.courierStock.CourierStockItem;
import uz.uwon.pharm.courierStock.CourierStockRepository;
import uz.uwon.pharm.courierStock.CourierStockService;
import uz.uwon.pharm.courierStock.ReturnDTO;
import uz.uwon.pharm.store.Store;
import uz.uwon.pharm.store.StoreDTO;
import uz.uwon.pharm.store.StoreRepository;
import uz.uwon.pharm.store.StoreService;
import uz.uwon.pharm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class StoreCourierStockFlowTest {

    private static final Long COURIER_ID = 2L;
    private static final Long PRODUCT_ID = 1L;

    @Autowired
    private StoreService storeService;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private CourierStockService courierStockService;
    @Autowired
    private CourierStockRepository courierStockRepository;

    @Test
    void arrivalThenTransferThenReturnKeepsQuantitiesConsistent() {
        Store store = store();
        BigDecimal storeBefore = store.getQuantity();
        BigDecimal courierBefore = courierQty();

        StoreDTO arrival = new StoreDTO();
        arrival.setQuantity(new BigDecimal("5"));
        arrival.setDateOfArrival(LocalDate.now());
        arrival.setComment("test kirim");
        storeService.saveArrivalProduct(store.getId(), arrival);
        storeRepository.flush();

        assertBd(storeBefore.add(new BigDecimal("5")), store().getQuantity());

        courierStockService.save(CourierStockDTO.builder()
                .courierId(COURIER_ID)
                .items(List.of(new CourierStockItem(PRODUCT_ID, new BigDecimal("3"))))
                .build());
        storeRepository.flush();

        assertBd(storeBefore.add(new BigDecimal("2")), store().getQuantity());
        assertBd(courierBefore.add(new BigDecimal("3")), courierQty());

        ReturnDTO ret = new ReturnDTO();
        ret.setCourierId(COURIER_ID);
        ret.setProductId(PRODUCT_ID);
        ret.setQuantity(new BigDecimal("2"));
        courierStockService.returnToStore(ret);
        storeRepository.flush();

        assertBd(storeBefore.add(new BigDecimal("4")), store().getQuantity());
        assertBd(courierBefore.add(new BigDecimal("1")), courierQty());
    }

    @Test
    void transferFailsWhenStoreHasNotEnoughStock() {
        BigDecimal available = store().getQuantity();
        BigDecimal requested = available.add(BigDecimal.ONE);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                courierStockService.save(CourierStockDTO.builder()
                        .courierId(COURIER_ID)
                        .items(List.of(new CourierStockItem(PRODUCT_ID, requested)))
                        .build()));
        assertTrue(ex.getMessage().toLowerCase().contains("not enough"));
        assertBd(available, store().getQuantity());
    }

    @Test
    void returnFailsWhenCourierHasNotEnoughStock() {
        BigDecimal available = courierQty();
        ReturnDTO ret = new ReturnDTO();
        ret.setCourierId(COURIER_ID);
        ret.setProductId(PRODUCT_ID);
        ret.setQuantity(available.add(BigDecimal.ONE));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                courierStockService.returnToStore(ret));
        assertTrue(ex.getMessage().toLowerCase().contains("not enough"));
        assertBd(available, courierQty());
    }

    private Store store() {
        return storeRepository.findByProduct_IdAndStatus(PRODUCT_ID, Status.ACTIVE).orElseThrow();
    }

    private BigDecimal courierQty() {
        return courierStockRepository.findByCourierIdAndProductId(COURIER_ID, PRODUCT_ID)
                .map(CourierStock::getQuantity)
                .orElse(BigDecimal.ZERO);
    }

    private static void assertBd(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }
}
