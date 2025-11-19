package org.igdevx.spring_boot_microservice_boilerplate.repository;

import org.igdevx.spring_boot_microservice_boilerplate.entity.OrderPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderPaymentRepository extends JpaRepository<OrderPayment, UUID> {
    
    Optional<OrderPayment> findByOrderId(String orderId);
    
    Optional<OrderPayment> findByPaymentIntentId(String paymentIntentId);
    
    boolean existsByOrderId(String orderId);
    
    boolean existsByPaymentIntentId(String paymentIntentId);
}