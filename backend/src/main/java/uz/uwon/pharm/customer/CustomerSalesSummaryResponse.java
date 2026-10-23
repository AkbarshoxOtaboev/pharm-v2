package uz.uwon.pharm.customer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CustomerSalesSummaryResponse {
    private long totalCustomers;
    private long activeBase;
    private long noOrders;
}
