package uz.uwon.pharm.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uz.uwon.pharm.cashRegister.CashRegisterDTO;
import uz.uwon.pharm.cashRegister.CashRegisterService;
import uz.uwon.pharm.courierStock.CourierStock;
import uz.uwon.pharm.courierStock.CourierStockRepository;
import uz.uwon.pharm.customer.Customer;
import uz.uwon.pharm.customer.CustomerRepository;
import uz.uwon.pharm.exceptions.NotFoundException;
import uz.uwon.pharm.orderDeliveridinfo.OrderDeliveredInfo;
import uz.uwon.pharm.orderDeliveridinfo.OrderDeliveredInfoRepository;
import uz.uwon.pharm.orderitem.OrderItem;
import uz.uwon.pharm.orderitem.OrderItemDTO;
import uz.uwon.pharm.orderitem.OrderItemResponse;
import uz.uwon.pharm.product.Product;
import uz.uwon.pharm.product.ProductRepository;
import uz.uwon.pharm.productSaleLog.SaleLogDTO;
import uz.uwon.pharm.productSaleLog.SaleLogService;
import uz.uwon.pharm.storage.StorageService;
import uz.uwon.pharm.users.*;
import uz.uwon.pharm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderServiceImplement implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final OrderDeliveredInfoRepository orderDeliveredInfoRepository;
    private final CourierStockRepository courierStockRepository;
    private final CashRegisterService cashRegisterService;
    private final SaleLogService saleLogService;
    private final UserService userService;

    @Override
    public void save(OrderDTO dto) {

        // =========================
        // 1. CUSTOMER
        // =========================
        Customer customer;

        if (dto.getCustomerId() == null) {
            customer = new Customer();
            customer.setFullName(dto.getFullName());
            customer.setPhone(dto.getPhone());
            customerRepository.save(customer);
        } else {
            customer = customerRepository.findByIdAndStatus(dto.getCustomerId(), Status.ACTIVE)
                    .orElseThrow(() -> new NotFoundException("Customer not found"));
        }

        // =========================
        // 2. ORDER ITEMS + STOCK CHECK
        // =========================
        BigDecimal totalSum = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemDTO itemDTO : dto.getItems()) {

            Product product = productRepository
                    .findProductByIdAndStatus(itemDTO.getProductId(), Status.ACTIVE)
                    .orElseThrow(() -> new NotFoundException("Product not found"));

            CourierStock stock = courierStockRepository
                    .findByCourierIdAndProductId(dto.getCourierId(), itemDTO.getProductId())
                    .orElseThrow(() -> new NotFoundException("Courier stock not found"));

            BigDecimal quantity = itemDTO.getQuantity();
            BigDecimal availableQty = stock.getQuantity();
            BigDecimal priceCost = product.getPriceCost();

            // =========================
            // ❗ STOCK VALIDATION
            // =========================
            if (availableQty.compareTo(quantity) < 0) {
                throw new RuntimeException(
                        "Mahsulot yetarli emas. Product: " + product.getName()
                                + ", mavjud: " + availableQty
                                + ", kerak: " + quantity
                );
            }

            // =========================
            // ORDER ITEM
            // =========================
            OrderItem orderItem;

            if (!itemDTO.getIsBonus()) {
                BigDecimal total = quantity.multiply(priceCost);
                totalSum = totalSum.add(total);

                orderItem = OrderItem.builder()
                        .product(product)
                        .price(priceCost)
                        .quantity(quantity)
                        .totalSum(total)
                        .isBonus(false)
                        .build();
            } else {
                orderItem = OrderItem.builder()
                        .product(product)
                        .price(priceCost)
                        .quantity(quantity)
                        .totalSum(BigDecimal.ZERO)
                        .isBonus(true)
                        .build();
            }

            orderItems.add(orderItem);

            // =========================
            // STOCK UPDATE
            // =========================
            BigDecimal newQty = availableQty.subtract(quantity);
            BigDecimal newTotalAmount = newQty.multiply(stock.getProductPrice());

            stock.setQuantity(newQty);
            stock.setTotalAmount(newTotalAmount);
        }

        // =========================
        // 3. ORDER
        // =========================
        User courier = userRepository.findById(dto.getCourierId())
                .orElseThrow(() -> new NotFoundException("Courier not found"));

        Order order = Order.builder()
                .customer(customer)
                .totalSum(totalSum)
                .address(dto.getAddress())
                .home(dto.getHome())
                .entrance(dto.getEntrance())
                .floor(dto.getFloor())
                .apartment(dto.getApartment())
                .user(courier)
                .paymentType(dto.getPaymentType())
                .orderStatus(OrderStatus.SHIPPED)
                .build();

        // =========================
        // 4. LINK ITEMS
        // =========================
        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }

        order.setOrderItems(orderItems);

        // =========================
        // 5. SAVE
        // =========================
        orderRepository.save(order);
    }

    @Override
    public List<OrderResponse> findAll() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderDetailResponse orderDetailResponses(Long orderId) {
        Order order = orderRepository.findOrderById(orderId).orElseThrow(() -> new NotFoundException("Order not found with id"));
        return mapToDetailResponse(order);
    }

    @Override
    public OrderPageResponse<OrderResponse> getOrders(
            Long id,
            String phone,
            OrderStatus status,
            LocalDate from,
            LocalDate to,
            int page,
            int size,
            Long userId
    ) {

        Specification<Order> spec = OrderSpecification.filter(id, phone, status, from, to, userId);

        // 🔒 exclude DELETED for VIEWER
        if (userService.getCurrentUser().getRole().equals(Role.VIEWER)) {
            spec = spec.and((root, query, cb) ->
                    cb.notEqual(root.get("orderStatus"), OrderStatus.DELETED));
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "id");

        // 🔥 ALL case
        if (size == -1) {
            List<Order> orders = orderRepository.findAll(spec, sort);

            return new OrderPageResponse<>(
                    orders.stream().map(this::mapToOrderResponse).toList(),
                    1,
                    orders.size(),
                    0
            );
        }

        // 📄 Paginated case
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Order> ordersPage = orderRepository.findAll(spec, pageable);

        return new OrderPageResponse<>(
                ordersPage.getContent().stream()
                        .map(this::mapToOrderResponse)
                        .toList(),
                ordersPage.getTotalPages(),
                ordersPage.getTotalElements(),
                ordersPage.getNumber()
        );
    }
    @Override
    public void updateStatus(Long id, String status) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        OrderStatus currentStatus = order.getOrderStatus();
        OrderStatus newStatus = OrderStatus.valueOf(status);

        if (EnumSet.of(
                OrderStatus.CANCELLED,
                OrderStatus.DELETED,
                OrderStatus.DELIVERED
        ).contains(currentStatus)) {

            log.error("Order already finished");
            return;
        }

        List<OrderItem> items = order.getOrderItems();

        // 🔴 CANCELLED yoki DELETED
        if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.DELETED) {

            for (OrderItem item : items) {
                CourierStock stock = courierStockRepository
                        .findByCourierIdAndProductId(order.getUser().getId(), item.getProduct().getId())
                        .orElseThrow(() -> new NotFoundException("stock not found"));

                stock.setQuantity(stock.getQuantity().add(item.getQuantity()));
                stock.setTotalAmount(
                        stock.getTotalAmount().add(
                                item.getQuantity().multiply(item.getProduct().getPrice())
                        )
                );
            }

            order.setOrderStatus(newStatus);
        }

        // 🟢 DELIVERED
        else if (newStatus == OrderStatus.DELIVERED) {

            order.setOrderStatus(newStatus);

            log.info("save cash");

            CashRegisterDTO dto = CashRegisterDTO.builder()
                    .courierId(order.getUser().getId())
                    .orderId(order.getId())
                    .orderTotalSum(order.getTotalSum())
                    .build();

            cashRegisterService.save(dto);

            for (OrderItem item : items) {

                log.info("save sale log");

                SaleLogDTO saleLogDTO = SaleLogDTO.builder()
                        .orderId(order.getId())
                        .categoryId(item.getProduct().getCategory().getId())
                        .categoryName(item.getProduct().getCategory().getName())
                        .customerName(order.getCustomer().getFullName())
                        .customerPhone(order.getCustomer().getPhone())
                        .customerAddress(order.getAddress())
                        .productName(item.getProduct().getName())
                        .productPriceCost(item.getPrice())
                        .quantity(item.getQuantity())
                        .totalSum(item.getQuantity().multiply(item.getProduct().getPriceCost()))
                        .isBonus(item.getIsBonus())
                        .build();

                saleLogService.save(saleLogDTO);
            }
        }

        // 🔵 boshqa statuslar
        else {
            order.setOrderStatus(newStatus);
        }
    }

    @Override
    public List<OrderDetailResponse> findOrdersByUserAndStatus(
            Long userId,
            OrderStatus orderStatus,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        LocalDateTime start;
        LocalDateTime end;

        if (startDate != null && endDate != null) {
            // custom range
            start = startDate;
            end = endDate;
        } else {
            // last 2 months (including current month)
            LocalDateTime now = LocalDateTime.now();

            start = now.minusMonths(1) // previous month
                    .withDayOfMonth(1)
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            end = now.plusMonths(1) // next month start
                    .withDayOfMonth(1)
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);
        }

        return orderRepository
                .findOrdersByUserIdAndStatusAndCreatedAtBetween(
                        userId,
                        orderStatus,
                        start,
                        end
                )
                .stream()
                .map(this::mapToDetailResponse)
                .toList();
    }
    @Override
    public List<OrderDetailResponse> fetchOrdersByCourierAndStatus(Long userId, OrderStatus orderStatus) {
        User user = userRepository.findById(userId).orElseThrow(()->new NotFoundException("Courier not found"));
        return orderRepository.findOrdersByUserAndOrderStatusOrderByIdDesc(user, orderStatus)
                .stream()
                .map(this::mapToDetailResponse)
                .toList();
    }

    @Override
    public void setOrderStatusAndComment(Long orderId, String status, String comment, MultipartFile file) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow();
        String fileName;
        if (file == null) {
            fileName = "no_image.png";
        } else {
            fileName = storageService.store(file);
        }
        OrderDeliveredInfo info = new OrderDeliveredInfo();
        info.setOrder(order);
        info.setComment(comment);
        info.setImage(fileName);
        info.setCreatedAt(LocalDateTime.now());
        orderDeliveredInfoRepository.save(info);
        // status update
       updateStatus(orderId, status);
    }

    @Override
    public OrderStatResponse orderStatResponse() {
        LocalDate today = LocalDate.now();

        LocalDate startDay = today;
        LocalDate endDay = today;

        LocalDate startMonth = today.withDayOfMonth(1);
        LocalDate endMonth = today.withDayOfMonth(today.lengthOfMonth());

        return orderRepository.getStatistics(startDay,endDay,startMonth,endMonth);
    }

    @Override
    public List<DailyStatDTO> getDailyStats() {

        LocalDate today = LocalDate.now();
        LocalDate start = today.withDayOfMonth(1);
        LocalDate end = today;

        List<DailyStatDTO> dbData = orderRepository.getDailyStats(start, end);

        Map<LocalDate, DailyStatDTO> map = dbData.stream()
                .collect(Collectors.toMap(DailyStatDTO::getDate, d -> d));

        List<DailyStatDTO> result = new ArrayList<>();

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {

            LocalDate finalD = d;
            result.add(
                    map.getOrDefault(d,
                            new DailyStatDTO() {
                                public LocalDate getDate() { return finalD; }
                                public Long getOrdersCount() { return 0L; }
                                public Long getDeliveredCount() { return 0L; }
                                public Long getCancelledCount() { return 0L; }
                                public BigDecimal getDeliveredSum() { return BigDecimal.ZERO; }
                                public BigDecimal getCancelledSum() { return BigDecimal.ZERO; }
                                public BigDecimal getDeliveredBonusSum() { return BigDecimal.ZERO; }
                            }
                    )
            );
        }

        return result;
    }

    @Override
    public List<BigDecimal> getMonthlySales(int year) {
        Map<Integer, BigDecimal> byMonth = orderRepository.getMonthlySales(year).stream()
                .collect(Collectors.toMap(
                        MonthlyStatDTO::getMonth,
                        d -> d.getTotalSum() != null ? d.getTotalSum() : BigDecimal.ZERO,
                        (a, b) -> a
                ));

        List<BigDecimal> result = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            result.add(byMonth.getOrDefault(m, BigDecimal.ZERO));
        }
        return result;
    }

    @Override
    public Long pendingOrderCount(Long courierId) {
        return orderRepository.pendingOrderCount(courierId);
    }

    @Override
    @Transactional
    public void update(Long id, OrderDTO dto) {

        // =========================
        // 🛑 VALIDATION
        // =========================
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new RuntimeException("Order must contain items");
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        Long courierId = order.getUser().getId();

        // =========================
        // 🔑 KEY CLASS
        // =========================
        class ItemKey {
            Long productId;
            Boolean isBonus;

            public ItemKey(Long productId, Boolean isBonus) {
                this.productId = productId;
                this.isBonus = isBonus;
            }

            public Long getProductId() {
                return productId;
            }

            public Boolean getIsBonus() {
                return isBonus;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof ItemKey)) return false;
                ItemKey key = (ItemKey) o;
                return Objects.equals(productId, key.productId) &&
                        Objects.equals(isBonus, key.isBonus);
            }

            @Override
            public int hashCode() {
                return Objects.hash(productId, isBonus);
            }
        }

        // =========================
        // 📦 PREPARE IDS
        // =========================
        List<Long> productIds = dto.getItems()
                .stream()
                .map(OrderItemDTO::getProductId)
                .distinct()
                .toList();

        // =========================
        // 📦 LOAD DATA
        // =========================
        Map<Long, Product> productMap = productRepository
                .findAllById(productIds)
                .stream()
                .filter(p -> p.getStatus() == Status.ACTIVE)
                .collect(Collectors.toMap(Product::getId, p -> p));

        Map<Long, CourierStock> stockMap = courierStockRepository
                .findAllByCourierIdAndProductIdIn(courierId, productIds)
                .stream()
                .collect(Collectors.toMap(
                        CourierStock::getId, // ✅ FIX
                        s -> s,
                        (a, b) -> a
                ));

        // =========================
        // 📦 OLD ITEMS MAP
        // =========================
        Map<ItemKey, OrderItem> oldItemsMap = order.getOrderItems()
                .stream()
                .collect(Collectors.toMap(
                        i -> new ItemKey(i.getProduct().getId(), i.getIsBonus()),
                        i -> i,
                        (a, b) -> a
                ));

        // =========================
        // 📦 NEW ITEMS MAP
        // =========================
        Map<ItemKey, OrderItemDTO> newItemsMap = dto.getItems()
                .stream()
                .collect(Collectors.toMap(
                        i -> new ItemKey(i.getProductId(), i.getIsBonus()),
                        i -> i,
                        (a, b) -> b
                ));

        // =========================
        // 🔴 REMOVE ITEMS
        // =========================
        for (ItemKey key : oldItemsMap.keySet()) {

            if (!newItemsMap.containsKey(key)) {

                OrderItem oldItem = oldItemsMap.get(key);
                CourierStock stock = stockMap.get(key.getProductId());

                if (stock == null) {
                    throw new RuntimeException("Stock not found for product " + key.getProductId());
                }

                stock.setQuantity(
                        stock.getQuantity().add(oldItem.getQuantity())
                );
            }
        }

        // =========================
        // 🟢 ADD / UPDATE ITEMS
        // =========================
        BigDecimal totalSum = BigDecimal.ZERO;
        List<OrderItem> updatedItems = new ArrayList<>();

        for (Map.Entry<ItemKey, OrderItemDTO> entry : newItemsMap.entrySet()) {

            ItemKey key = entry.getKey();
            OrderItemDTO dtoItem = entry.getValue();

            Product product = productMap.get(key.getProductId());
            if (product == null) {
                throw new RuntimeException("Product not found: " + key.getProductId());
            }

            CourierStock stock = stockMap.get(key.getProductId());
            if (stock == null) {
                throw new RuntimeException("Stock not found: " + key.getProductId());
            }

            BigDecimal newQty = dtoItem.getQuantity();
            OrderItem oldItem = oldItemsMap.get(key);

            if (oldItem == null) {
                // 🆕 NEW

                if (!dtoItem.getIsBonus()) {
                    if (stock.getQuantity().compareTo(newQty) < 0) {
                        throw new RuntimeException("Not enough stock for product " + key.getProductId());
                    }
                    stock.setQuantity(stock.getQuantity().subtract(newQty));
                }

            } else {
                // 🔄 UPDATE

                BigDecimal oldQty = oldItem.getQuantity();
                BigDecimal diff = newQty.subtract(oldQty);

                if (!dtoItem.getIsBonus()) {

                    if (diff.compareTo(BigDecimal.ZERO) > 0) {
                        if (stock.getQuantity().compareTo(diff) < 0) {
                            throw new RuntimeException("Not enough stock for product " + key.getProductId());
                        }
                        stock.setQuantity(stock.getQuantity().subtract(diff));

                    } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                        stock.setQuantity(
                                stock.getQuantity().add(oldQty.subtract(newQty))
                        );
                    }
                }
            }

            BigDecimal price = product.getPriceCost();

            BigDecimal total = dtoItem.getIsBonus()
                    ? BigDecimal.ZERO
                    : newQty.multiply(price);

            totalSum = totalSum.add(total);

            OrderItem newItem = OrderItem.builder()
                    .product(product)
                    .quantity(newQty)
                    .price(price)
                    .totalSum(total)
                    .isBonus(dtoItem.getIsBonus())
                    .order(order)
                    .build();

            updatedItems.add(newItem);
        }

        // =========================
        // 💾 SAVE
        // =========================
        order.getOrderItems().clear();

        for (OrderItem item : updatedItems) {
            item.setOrder(order);
            order.getOrderItems().add(item);
        }

        order.setTotalSum(totalSum);

        orderRepository.save(order);
    }

    @Override
    public BigDecimal orderBonusSum(Long orderId) {
        Order order = orderRepository.findOrderById(orderId).orElseThrow();
        BigDecimal sum = BigDecimal.ZERO;
        for(OrderItem item : order.getOrderItems()) {
            if(Boolean.TRUE.equals(item.getIsBonus())){
               sum =  sum.add(item.getQuantity().multiply(item.getProduct().getPriceCost()));
            }
        }
        return sum;
    }


    private OrderResponse mapToOrderResponse(Order order) {
        BigDecimal orderBonusSum = orderBonusSum(order.getId());
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomer().getFullName(),
                order.getAddress(),
                order.getCustomer().getPhone(),
                order.getTotalSum(),
                orderBonusSum,
                order.getOrderStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getUser().getFullName()
        );
    }

    private OrderDetailResponse mapToDetailResponse(Order order) {
        UserResponse courier = new UserResponse(order.getUser().getId(), order.getUser().getFullName(), order.getUser().getWorkPhone());
        String phone = order.getCustomer().getPhone();
        if (phone != null && !phone.startsWith("+")) {
            phone = "+" + phone;
        }
        List<OrderItemResponse> items = new ArrayList<>();
        for (OrderItem item : order.getOrderItems()) {
            CourierStock stock = courierStockRepository.findByCourierIdAndProductId(courier.getId(), item.getProduct().getId()).orElseThrow();
            OrderItemResponse response = OrderItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProduct().getId())
                    .storeQuantity(stock.getQuantity())
                    .productName(item.getProduct().getName())
                    .price(item.getPrice())
                    .quantity(item.getQuantity())
                    .totalSum(item.getTotalSum())
                    .isBonus(item.getIsBonus())
                    .type(item.getProduct().getUnitType().getLabel())
                    .build();
            items.add(response);
        }
        return new OrderDetailResponse(
                order.getId(),
                order.getCustomer().getFullName(),
                phone,
                order.getOrderNumber(),
                order.getCreatedAt(),
                order.getAddress(),
                order.getHome(),
                order.getEntrance(),
                order.getApartment(),
                order.getFloor(),
                order.getOrientations(),
                order.getOrderStatus(),
                order.getTotalSum(),
                courier,
                items
        );
    }
}
