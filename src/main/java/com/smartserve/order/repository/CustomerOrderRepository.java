package com.smartserve.order.repository;

import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByOrderStatus(OrderStatus orderStatus);
    List<CustomerOrder> findByTableNumber(Integer tableNumber);
    List<CustomerOrder> findByTableNumberAndOrderStatus(Integer tableNumber, OrderStatus orderStatus);

}
