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
        try { realtimeGateway.products("PRODUCTS_CHANGED"); } catch (Exception ignore) {}
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
            "total", total
        );
    }

    @Transactional
    public OrderResponse accept(Long orderId) {
        OrderEntity order = requireOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Solo se pueden aceptar pedidos pendientes");
        }
        order.setStatus(OrderStatus.ACCEPTED);
        OrderEntity saved = orderRepository.save(order);
        publishOrderEvent("accepted", saved);
        return OrderMapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse ready(Long orderId) {
        OrderEntity order = requireOrder(orderId);
        if (order.getStatus() != OrderStatus.ACCEPTED) {
            throw new BadRequestException("Solo se pueden marcar como listos pedidos aceptados");
        }
        order.setStatus(OrderStatus.READY);
        OrderEntity saved = orderRepository.save(order);
        publishOrderEvent("ready", saved);
        return OrderMapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse deliver(Long orderId, String paymentReference) {
        OrderEntity order = requireOrder(orderId);
        if (order.getStatus() != OrderStatus.READY && order.getStatus() != OrderStatus.ACCEPTED) {
            throw new BadRequestException("Solo se pueden entregar pedidos listos o aceptados");
        }
        order.setPaymentReference(paymentReference);
        order.setStatus(OrderStatus.DELIVERED);

        // If payment was cash pending or in progress, mark as PAID on delivery
        if (order.getPaymentStatus() == PaymentStatus.PENDING_PAYMENT_CASH || order.getPaymentStatus() == PaymentStatus.PAYMENT_IN_PROGRESS) {
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
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("No se puede cancelar un pedido entregado o ya cancelado");
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
            log.debug("Override price for product {} from {} to {}", product.getId(), product.getPrice(), requestedPrice);
        }
    }

    private void publishOrderEvent(String type, OrderEntity order) {
        org.hibernate.Hibernate.initialize(order.getItems());
        var orderResponse = OrderMapper.toResponse(order);
        realtimeGateway.sales(java.util.Map.of(
            "type", type,
            "order", orderResponse
        ));
        // Feed notifications channel
        realtimeGateway.notifyChannel(java.util.Map.of(
            "type", type,
            "orderId", order.getId(),
            "status", order.getStatus()
        ));
        // Also publish to orders/<type> so frontend hooks receive specific events
        realtimeGateway.orders(type, java.util.Map.of(
            "type", "order." + type,
            "order", orderResponse
        ));
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
            case TRANSFER -> order.setPaymentStatus(PaymentStatus.PAYMENT_PENDING_CONF);
            case CASH -> order.setPaymentStatus(PaymentStatus.PENDING_PAYMENT_CASH);
            case CARD -> order.setPaymentStatus(PaymentStatus.PAYMENT_IN_PROGRESS);
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
            "order", OrderMapper.toResponse(order)
        );
        realtimeGateway.sales(payload);
        realtimeGateway.notifyChannel(payload);
    }
}
