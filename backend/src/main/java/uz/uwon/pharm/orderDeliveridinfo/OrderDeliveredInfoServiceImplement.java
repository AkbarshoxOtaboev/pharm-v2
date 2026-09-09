package uz.uwon.pharm.orderDeliveridinfo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.uwon.pharm.exceptions.NotFoundException;
import uz.uwon.pharm.order.Order;
import uz.uwon.pharm.order.OrderRepository;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderDeliveredInfoServiceImplement implements OrderDeliveredInfoService {

    private final OrderDeliveredInfoRepository repository;
    private final OrderRepository orderRepository;

    @Override
    public List<OrderDeliveredInfoResponse> getOrderDeliveredInfoResponses(Long orderId) {
        Order order = orderRepository.findOrderById(orderId).orElseThrow(()->new NotFoundException("Order not found with id"));
        return repository.findAllByOrderOrderByIdDesc(order).stream().map(this::mapToResponse).toList();
    }

    @Override
    public void save(Long orderId, String comment) {
        Order order = orderRepository.findOrderById(orderId).orElseThrow(()->new NotFoundException("Order not found with id"));
        OrderDeliveredInfo info = OrderDeliveredInfo.builder()
                .order(order)
                .comment(comment)
                .image("no_image.png")
                .build();
        repository.save(info);
    }

    private OrderDeliveredInfoResponse mapToResponse(OrderDeliveredInfo info){
        return new OrderDeliveredInfoResponse(
                info.getImage(),
                info.getComment(),
                info.getCreatedAt()
        );
    }
}
