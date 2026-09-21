package uz.uwon.pharm.statistics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uz.uwon.pharm.order.OrderRepository;
import uz.uwon.pharm.users.CourierStatsDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class CourierPaymentStatsTest {

    private static final Long COURIER_ID = 2L;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void cashAndCardTotalsAddUpToDeliveredTotal() {
        LocalDateTime start = LocalDateTime.now().minusYears(5);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        CourierStatsDto stats = orderRepository.getCourierStats(COURIER_ID, start, end);

        assertNotNull(stats);
        BigDecimal cash = stats.getCashTotal();
        BigDecimal card = stats.getCardTotal();
        assertNotNull(cash);
        assertNotNull(card);
        assertEquals(0, cash.add(card).compareTo(stats.getTotalSum()),
                "Naqd va karta summasi yetkazilgan buyurtmalar jamiga teng bo'lishi kerak");
    }
}
