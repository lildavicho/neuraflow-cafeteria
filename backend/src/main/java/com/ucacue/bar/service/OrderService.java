package com.ucacue.bar.service;

import com.ucacue.bar.dto.order.OrderCreateRequest;
import com.ucacue.bar.dto.order.OrderMapper;
import com.ucacue.bar.dto.order.OrderResponse;
import com.ucacue.bar.entity.OrderEntity;
import com.ucacue.bar.entity.OrderEntity.OrderStatus;
import com.ucacue.bar.entity.OrderEntity.PaymentMethod;
import com.ucacue.bar.entity.OrderEntity.PaymentStatus;
import com.ucacue.bar.entity.OrderItemEntity;
import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.entity.ProductEntity.ProductStatus;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.algo.PreparationTimePredictor;
import com.ucacue.bar.entity.MlPredictionEntity.PredictionType;
import com.ucacue.bar.repository.OrderItemRepository;
import com.ucacue.bar.repository.OrderRepository;
import com.ucacue.bar.repository.ProductRepository;
import com.ucacue.bar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final BigDecimal IVA_RATE = new BigDecimal("0.00");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final LoyaltyService loyaltyService;
    private final SaleService saleService;
    private final DashboardService dashboardService;
    private final RealtimeGateway realtimeGateway;
    private final NotificationService notificationService;
    private final PreparationTimePredictor preparationTimePredictor;
    private final MlPredictionService mlPredictionService;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (request.getItems().isEmpty()) {
            throw new BadRequestException("El pedido debe contener productos");
        }

        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().name()));
        order.setNotes(request.getNotes());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderCreateRequest.Item item : request.getItems()) {
            ProductEntity product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new NotFoundException("Producto no encontrado: " + item.getProductId()));

            validatePrice(product, item.getUnitPrice());

            // Stock validation and decrement
            int qty = item.getQuantity();
            if (qty <= 0) {
                throw new BadRequestException("Cantidad inválida para el producto " + product.getName());
            }
            if (product.getStock() < qty) {
                throw new BadRequestException("Stock insuficiente para " + product.getName());
            }
            int newStock = product.getStock() - qty;
            product.setStock(newStock);
            if (newStock <= 0) {
                product.setStatus(ProductStatus.SOLD_OUT);
            }
            productRepository.save(product);

            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setProduct(product);
            orderItem.setQuantity(qty);
            orderItem.setUnitPrice(item.getUnitPrice());
            BigDecimal lineTotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(qty))
                    .setScale(2, RoundingMode.HALF_UP);
            orderItem.setLineTotal(lineTotal);
            order.addItem(orderItem);
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal tax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.setScale(2, RoundingMode.HALF_UP);

        order.setSubtotal(subtotal);
        order.setTax(tax);
        order.setTotal(total);

        OrderEntity saved = orderRepository.save(order);
        orderItemRepository.saveAll(saved.getItems());

        log.info("Order {} created for {} with {} items", saved.getId(), user.getEmail(), saved.getItems().size());
        try {
            realtimeGateway.products("PRODUCTS_CHANGED");
        } catch (Exception ignore) {
        }
        publishOrderEvent("created", saved);
        dashboardService.publishSnapshotAsync();
        return OrderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> list(OrderStatus status, Pageable pageable) {
        Page<OrderEntity> orders = status != null
                ? orderRepository.findByStatus(status, pageable)
                : orderRepository.findAll(pageable);
        return orders.map(OrderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        OrderEntity order = requireOrder(id);
        org.hibernate.Hibernate.initialize(order.getItems()); // initialize lazy collection
        return OrderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> previewTotals(java.util.List<OrderCreateRequest.Item> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderCreateRequest.Item item : items) {
            ProductEntity product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new NotFoundException("Producto no encontrado: " + item.getProductId()));
            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : product.getPrice();
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        BigDecimal tax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.setScale(2, RoundingMode.HALF_UP);
        return java.util.Map.of(
                "subtotal", subtotal,
                "tax", tax,
                "total", total);
    }

    @Transactional
    public OrderResponse confirm(Long orderId) {
        OrderEntity order = requireOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Solo se pueden confirmar pedidos pendientes");
        }
        order.setStatus(OrderStatus.CONFIRMED);
        OrderEntity saved = orderRepository.save(order);
        publishOrderEvent("confirmed", saved);
        return OrderMapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse prepare(Long orderId) {
        OrderEntity order = requireOrder(orderId);
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Solo se pueden preparar pedidos confirmados");
        }
        order.setStatus(OrderStatus.PREPARING);
        if (order.getPreparationStartAt() == null) {
            order.setPreparationStartAt(LocalDateTime.now());
        }
        PreparationTimePredictor.Estimate estimate = preparationTimePredictor.estimate(order.getId());
        order.setEstimatedReadyAt(order.getPreparationStartAt().plusMinutes(estimate.getEstimatedMinutes()));
        OrderEntity saved = orderRepository.save(order);
        mlPredictionService.createPrediction(saved, PredictionType.PREPARATION_TIME,
                String.valueOf(estimate.getEstimatedMinutes()), estimate.getConfidence());
        notificationService.notifyEtaUpdate(saved, estimate.getEstimatedMinutes());
        publishOrderEvent("preparing", saved);
        return OrderMapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse ready(Long orderId) {
        OrderEntity order = requireOrder(orderId);
        if (order.getStatus() != OrderStatus.PREPARING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Solo se pueden marcar como listos pedidos en preparación o confirmados");
        }
        order.setStatus(OrderStatus.READY);
        if (order.getPreparationStartAt() == null) {
            order.setPreparationStartAt(LocalDateTime.now());
        }
        order.setActualReadyAt(LocalDateTime.now());
        OrderEntity saved = orderRepository.save(order);
        Integer estimatedMinutes = estimateMinutesFromOrder(saved);
        notificationService.notifyOrderReady(saved, estimatedMinutes);
        if (saved.getPreparationStartAt() != null && saved.getActualReadyAt() != null) {
            long actualMinutes = Duration.between(saved.getPreparationStartAt(), saved.getActualReadyAt()).toMinutes();
            mlPredictionService.updateActualValue(saved.getId(), PredictionType.PREPARATION_TIME,
                    String.valueOf(actualMinutes));
        }
        publishOrderEvent("ready", saved);
        return OrderMapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse deliver(Long orderId, String paymentReference) {
        OrderEntity order = requireOrder(orderId);
        if (order.getStatus() != OrderStatus.READY && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Solo se pueden entregar pedidos listos o confirmados");
        }
        order.setPaymentReference(paymentReference);
        order.setStatus(OrderStatus.DELIVERED);

        // Si el pago estaba pendiente, marcarlo como pagado al entregar
        if (order.getPaymentStatus() == PaymentStatus.PENDING) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        int points = loyaltyService.awardForOrder(order);
        order.setLoyaltyPointsAwarded(points);

        saleService.registerSale(order);

        OrderEntity saved = orderRepository.save(order);
        publishOrderEvent("delivered", saved);
        publishPaymentEvent(saved);
        dashboardService.publishSnapshotAsync();
        return OrderMapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse cancel(Long orderId, String reason) {
        OrderEntity order = requireOrder(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("No se puede cancelar un pedido completado, entregado o ya cancelado");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setNotes(reason);
        OrderEntity saved = orderRepository.save(order);
        publishOrderEvent("cancelled", saved);
        dashboardService.publishSnapshotAsync();
        return OrderMapper.toResponse(saved);
    }

    private OrderEntity requireOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado"));
    }

    private void validatePrice(ProductEntity product, BigDecimal requestedPrice) {
        if (requestedPrice == null) {
            throw new BadRequestException("El precio unitario es obligatorio");
        }
        if (product.getPrice().compareTo(requestedPrice) != 0) {
            log.debug("Override price for product {} from {} to {}", product.getId(), product.getPrice(),
                    requestedPrice);
        }
    }

    private void publishOrderEvent(String type, OrderEntity order) {
        org.hibernate.Hibernate.initialize(order.getItems());
        var orderResponse = OrderMapper.toResponse(order);
        Integer estimatedMinutes = estimateMinutesFromOrder(order);
        realtimeGateway.sales(java.util.Map.of(
                "type", type,
                "order", orderResponse));
        // Feed notifications channel
        var notifyPayload = new java.util.HashMap<String, Object>();
        notifyPayload.put("type", type);
        notifyPayload.put("orderId", order.getId());
        notifyPayload.put("status", order.getStatus());
        notifyPayload.put("estimatedMinutes", estimatedMinutes);
        notifyPayload.put("estimatedReadyAt", order.getEstimatedReadyAt());
        notifyPayload.put("preparationStartAt", order.getPreparationStartAt());
        notifyPayload.put("actualReadyAt", order.getActualReadyAt());
        if (order.getStatus() == OrderStatus.READY) {
            notifyPayload.put("pickupLocation", "Mostrador Principal");
        }
        realtimeGateway.notifyChannel(notifyPayload);
        // Also publish to orders/<type> so frontend hooks receive specific events
        realtimeGateway.orders(type, java.util.Map.of(
                "type", "order." + type,
                "order", orderResponse));
    }

    private Integer estimateMinutesFromOrder(OrderEntity order) {
        if (order.getPreparationStartAt() == null || order.getEstimatedReadyAt() == null) {
            return null;
        }
        long minutes = Duration.between(order.getPreparationStartAt(), order.getEstimatedReadyAt()).toMinutes();
        if (minutes <= 0) {
            return null;
        }
        return (int) minutes;
    }

    @Transactional
    public OrderResponse pay(Long orderId, String method, String reference) {
        OrderEntity order = requireOrder(orderId);
        PaymentMethod pm;
        try {
            pm = PaymentMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Método de pago inválido");
        }
        order.setPaymentMethod(pm);
        if (reference != null && !reference.isBlank()) {
            order.setPaymentReference(reference);
        }

        switch (pm) {
            case TRANSFER, CASH, CARD -> order.setPaymentStatus(PaymentStatus.PENDING);
            default -> throw new BadRequestException("Método de pago no soportado");
        }

        OrderEntity saved = orderRepository.save(order);
        publishPaymentEvent(saved);
        return OrderMapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse confirmPayment(Long orderId, String reference) {
        OrderEntity order = requireOrder(orderId);
        if (reference != null && !reference.isBlank()) {
            order.setPaymentReference(reference);
        }
        order.setPaymentStatus(PaymentStatus.PAID);
        OrderEntity saved = orderRepository.save(order);
        // Award loyalty and register sale upon payment confirmation
        int points = loyaltyService.awardForOrder(saved);
        saved.setLoyaltyPointsAwarded(points);
        saleService.registerSale(saved);
        publishPaymentEvent(saved);
        dashboardService.publishSnapshotAsync();
        return OrderMapper.toResponse(saved);
    }

    private void publishPaymentEvent(OrderEntity order) {
        var payload = java.util.Map.of(
                "type", "ORDER_PAYMENT_UPDATED",
                "order", OrderMapper.toResponse(order));
        realtimeGateway.sales(payload);
        realtimeGateway.notifyChannel(payload);
    }
}
