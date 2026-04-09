package com.ucacue.bar.service;

import com.ucacue.bar.algo.PreparationTimePredictor;
import com.ucacue.bar.dto.order.OrderCreateRequest;
import com.ucacue.bar.dto.order.OrderMapper;
import com.ucacue.bar.dto.order.OrderResponse;
import com.ucacue.bar.entity.MlPredictionEntity.PredictionType;
import com.ucacue.bar.entity.OrderEntity;
import com.ucacue.bar.entity.OrderEntity.InventoryStatus;
import com.ucacue.bar.entity.OrderEntity.OrderStatus;
import com.ucacue.bar.entity.OrderEntity.PaymentMethod;
import com.ucacue.bar.entity.OrderEntity.PaymentStatus;
import com.ucacue.bar.entity.OrderEntity.TransactionStatus;
import com.ucacue.bar.entity.OrderItemEntity;
import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.entity.ProductEntity.ProductStatus;
import com.ucacue.bar.entity.StockMovementEntity;
import com.ucacue.bar.entity.StockMovementEntity.MovementType;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.erp.accounting.application.TaxComputationService;
import com.ucacue.bar.erp.accounting.application.TaxComputationService.TaxSummary;
import com.ucacue.bar.erp.accounting.application.SalesAccountingService;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.repository.OrderItemRepository;
import com.ucacue.bar.repository.OrderRepository;
import com.ucacue.bar.repository.ProductRepository;
import com.ucacue.bar.repository.StockMovementRepository;
import com.ucacue.bar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final String ORDER_REFERENCE_TYPE = "ORDER";
    private static final String DEFAULT_CANCEL_REASON = "Cancelacion de orden";
    private static final String DEFAULT_REFUND_REASON = "Reembolso de orden";
    private static final String DEFAULT_RESERVATION_REASON = "Reserva de inventario por orden";
    private static final String DEFAULT_COMMIT_REASON = "Descuento definitivo por pago de orden";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final LoyaltyService loyaltyService;
    private final SaleService saleService;
    private final DashboardService dashboardService;
    private final RealtimeGateway realtimeGateway;
    private final NotificationService notificationService;
    private final PreparationTimePredictor preparationTimePredictor;
    private final MlPredictionService mlPredictionService;
    private final TaxComputationService taxComputationService;
    private final TenantContextResolver tenantContextResolver;
    private final SalesAccountingService salesAccountingService;

    @Transactional
    @CacheEvict(cacheNames = "products-public", allEntries = true)
    public OrderResponse createOrder(OrderCreateRequest request, String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (request.getItems().isEmpty()) {
            throw new BadRequestException("El pedido debe contener productos");
        }

        Map<Long, ProductEntity> lockedProducts = lockProducts(extractProductIds(request.getItems()));
        validateReservableItems(request.getItems(), lockedProducts);

        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().name()));
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTransactionStatus(TransactionStatus.PENDING_PAYMENT);
        order.setPaidAt(null);
        order.setCancelledAt(null);
        order.setRefundedAt(null);
        order.setInventoryStatus(InventoryStatus.RESERVED);
        order.setNotes(request.getNotes());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderCreateRequest.Item item : request.getItems()) {
            ProductEntity product = lockedProducts.get(item.getProductId());
            validateItem(product, item);

            int qty = item.getQuantity();
            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setProduct(product);
            orderItem.setQuantity(qty);
            orderItem.setUnitPrice(item.getUnitPrice());

            BigDecimal grossLineTotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(qty))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal discountAmount = sanitizeDiscount(item.getDiscountAmount(), grossLineTotal);
            BigDecimal lineTotal = grossLineTotal.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
            orderItem.setLineTotal(lineTotal);

            order.addItem(orderItem);
            subtotal = subtotal.add(lineTotal);
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.15d)).setScale(2, RoundingMode.HALF_UP);
        order.setSubtotal(subtotal);
        order.setTax(tax);
        order.setTotal(subtotal.add(tax).setScale(2, RoundingMode.HALF_UP));

        OrderEntity saved = orderRepository.save(order);
        orderItemRepository.saveAll(saved.getItems());
        recordReservationMovements(saved, lockedProducts);

        log.info("Order {} created for {} with {} items and reserved inventory", saved.getId(), user.getEmail(),
                saved.getItems().size());
        publishAvailabilityChange();
        publishOrderEvent("created", saved);
        dashboardService.publishSnapshotAsync();
        return OrderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> list(OrderStatus status, TransactionStatus transactionStatus, Pageable pageable) {
        Page<OrderEntity> orders = transactionStatus != null
                ? orderRepository.findByTransactionStatus(transactionStatus, pageable)
                : status != null
                ? orderRepository.findByStatus(status, pageable)
                : orderRepository.findAll(pageable);
        return orders.map(OrderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        OrderEntity order = requireOrder(id);
        org.hibernate.Hibernate.initialize(order.getItems());
        return OrderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> previewTotals(List<OrderCreateRequest.Item> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderCreateRequest.Item item : items) {
            ProductEntity product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new NotFoundException("Producto no encontrado: " + item.getProductId()));
            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : product.getPrice();
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        TaxSummary taxSummary = taxComputationService.calculate(
                tenantContextResolver.resolveDefault().getId(),
                items.stream()
                        .map(item -> new TaxComputationService.TaxableLine(
                                item.getProductId(),
                                item.getQuantity(),
                                item.getUnitPrice() != null ? item.getUnitPrice() : productRepository.findById(item.getProductId())
                                        .orElseThrow(() -> new NotFoundException("Producto no encontrado: " + item.getProductId()))
                                        .getPrice()))
                        .toList());
        return Map.of(
                "subtotal", taxSummary.subtotal(),
                "tax", taxSummary.taxAmount(),
                "total", taxSummary.total(),
                "taxLines", taxSummary.lines());
    }

    @Transactional
    public OrderResponse confirm(Long orderId) {
        OrderEntity order = requireOrderForUpdate(orderId);
        ensureMutable(order);
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
        OrderEntity order = requireOrderForUpdate(orderId);
        ensureMutable(order);
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
        OrderEntity order = requireOrderForUpdate(orderId);
        ensureMutable(order);
        if (order.getStatus() != OrderStatus.PREPARING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Solo se pueden marcar como listos pedidos en preparacion o confirmados");
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
        OrderEntity order = requireOrderForUpdate(orderId);
        ensureMutable(order);
        if (order.getStatus() != OrderStatus.READY && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Solo se pueden entregar pedidos listos o confirmados");
        }
        if (order.getTransactionStatus() != TransactionStatus.PAID) {
            throw new BadRequestException("No se puede entregar un pedido sin pago confirmado");
        }
        if (paymentReference != null && !paymentReference.isBlank()) {
            order.setPaymentReference(paymentReference);
        }
        order.setStatus(OrderStatus.DELIVERED);

        OrderEntity saved = orderRepository.save(order);
        publishOrderEvent("delivered", saved);
        dashboardService.publishSnapshotAsync();
        return OrderMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = "products-public", allEntries = true)
    public OrderResponse cancel(Long orderId, String reason) {
        OrderEntity order = requireOrderForUpdate(orderId);
        if (order.getTransactionStatus() == TransactionStatus.CANCELLED
                || order.getTransactionStatus() == TransactionStatus.REFUNDED) {
            throw new BadRequestException("La orden ya esta cancelada o reembolsada");
        }
        if (order.getTransactionStatus() == TransactionStatus.PAID) {
            throw new BadRequestException("Una orden pagada debe pasar por reembolso, no por cancelacion");
        }
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("No se puede cancelar un pedido completado, entregado o ya cancelado");
        }

        InventoryStatus previousInventoryStatus = order.getInventoryStatus();
        releaseInventory(order, reason);
        order.setPaymentStatus(PaymentStatus.FAILED);

        order.setStatus(OrderStatus.CANCELLED);
        order.setTransactionStatus(TransactionStatus.CANCELLED);
        order.setInventoryStatus(InventoryStatus.RELEASED);
        order.setCancelledAt(LocalDateTime.now());
        order.setNotes(reason);

        OrderEntity saved = orderRepository.save(order);
        if (previousInventoryStatus != InventoryStatus.RELEASED) {
            publishAvailabilityChange();
        }
        publishOrderEvent("cancelled", saved);
        publishPaymentEvent(saved);
        dashboardService.publishSnapshotAsync();
        return OrderMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = "products-public", allEntries = true)
    public OrderResponse refund(Long orderId, String reason) {
        OrderEntity order = requireOrderForUpdate(orderId);
        if (order.getTransactionStatus() != TransactionStatus.PAID) {
            throw new BadRequestException("Solo se pueden reembolsar ordenes pagadas");
        }

        restoreInventoryAfterRefund(order, reason);
        order.setStatus(OrderStatus.CANCELLED);
        order.setTransactionStatus(TransactionStatus.REFUNDED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setRefundedAt(LocalDateTime.now());
        order.setNotes(reason);

        saleService.markSaleRefunded(order);
        loyaltyService.revokeForOrder(order);

        OrderEntity saved = orderRepository.save(order);
        salesAccountingService.postRefund(saved);
        publishAvailabilityChange();
        publishOrderEvent("refunded", saved);
        publishPaymentEvent(saved);
        dashboardService.publishSnapshotAsync();
        return OrderMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = "products-public", allEntries = true)
    public OrderResponse pay(Long orderId, String method, String reference) {
        return pay(orderId, method, reference, null);
    }

    @Transactional
    @CacheEvict(cacheNames = "products-public", allEntries = true)
    public OrderResponse pay(Long orderId, String method, String reference, String paymentBreakdownJson) {
        OrderEntity order = requireOrderForUpdate(orderId);
        if (order.getTransactionStatus() == TransactionStatus.CANCELLED
                || order.getTransactionStatus() == TransactionStatus.REFUNDED) {
            throw new BadRequestException("No se puede pagar una orden cancelada o reembolsada");
        }

        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Metodo de pago invalido");
        }

        order.setPaymentMethod(paymentMethod);
        if (reference != null && !reference.isBlank()) {
            order.setPaymentReference(reference);
        }
        if (paymentBreakdownJson != null && !paymentBreakdownJson.isBlank()) {
            order.setPaymentBreakdownJson(paymentBreakdownJson);
        }
        boolean hasPendingTransferBreakdown = hasPendingTransferBreakdown(order.getPaymentBreakdownJson());

        return switch (paymentMethod) {
            case CASH, CARD -> {
                if (hasPendingTransferBreakdown) {
                    order.setPaymentStatus(PaymentStatus.PENDING);
                    OrderEntity saved = orderRepository.save(order);
                    publishPaymentEvent(saved);
                    yield OrderMapper.toResponse(saved);
                }
                yield finalizePayment(order);
            }
            case TRANSFER -> {
                order.setPaymentStatus(PaymentStatus.PENDING);
                OrderEntity saved = orderRepository.save(order);
                publishPaymentEvent(saved);
                yield OrderMapper.toResponse(saved);
            }
        };
    }

    @Transactional
    @CacheEvict(cacheNames = "products-public", allEntries = true)
    public OrderResponse confirmPayment(Long orderId, String reference) {
        OrderEntity order = requireOrderForUpdate(orderId);
        if (order.getTransactionStatus() == TransactionStatus.CANCELLED
                || order.getTransactionStatus() == TransactionStatus.REFUNDED) {
            throw new BadRequestException("No se puede confirmar el pago de una orden cancelada o reembolsada");
        }
        if (reference != null && !reference.isBlank()) {
            order.setPaymentReference(reference);
        }
        return finalizePayment(order);
    }

    private OrderEntity requireOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado"));
    }

    private OrderEntity requireOrderForUpdate(Long id) {
        return orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado"));
    }

    private OrderResponse finalizePayment(OrderEntity order) {
        if (order.getTransactionStatus() == TransactionStatus.PAID) {
            return OrderMapper.toResponse(order);
        }
        if (order.getInventoryStatus() == InventoryStatus.RELEASED) {
            throw new BadRequestException("La orden ya no tiene inventario reservado");
        }

        boolean inventoryCommitted = false;
        if (order.getInventoryStatus() == InventoryStatus.RESERVED) {
            commitReservedInventory(order);
            order.setInventoryStatus(InventoryStatus.COMMITTED);
            inventoryCommitted = true;
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setTransactionStatus(TransactionStatus.PAID);
        if (order.getPaidAt() == null) {
            order.setPaidAt(LocalDateTime.now());
        }

        if (order.getLoyaltyPointsAwarded() == null) {
            int points = loyaltyService.awardForOrder(order);
            order.setLoyaltyPointsAwarded(points);
        }

        saleService.registerSale(order);

        OrderEntity saved = orderRepository.save(order);
        salesAccountingService.postPaidOrder(saved);
        if (inventoryCommitted) {
            publishAvailabilityChange();
        }
        publishPaymentEvent(saved);
        dashboardService.publishSnapshotAsync();
        return OrderMapper.toResponse(saved);
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

    private BigDecimal sanitizeDiscount(BigDecimal requestedDiscount, BigDecimal grossLineTotal) {
        BigDecimal discount = requestedDiscount != null
                ? requestedDiscount.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("El descuento no puede ser negativo");
        }
        if (discount.compareTo(grossLineTotal) > 0) {
            throw new BadRequestException("El descuento no puede superar el subtotal de la línea");
        }
        return discount;
    }

    private boolean hasPendingTransferBreakdown(String paymentBreakdownJson) {
        if (paymentBreakdownJson == null || paymentBreakdownJson.isBlank()) {
            return false;
        }
        return paymentBreakdownJson.contains("\"method\":\"TRANSFER\"")
                || paymentBreakdownJson.contains("\"method\": \"TRANSFER\"");
    }

    private void validateItem(ProductEntity product, OrderCreateRequest.Item item) {
        if (product == null) {
            throw new BadRequestException("Producto no encontrado");
        }
        if (product.getStatus() == ProductStatus.DISCONTINUED) {
            throw new BadRequestException("El producto " + product.getName() + " esta descontinuado");
        }
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new BadRequestException("Cantidad invalida para el producto " + product.getName());
        }
        validatePrice(product, item.getUnitPrice());
    }

    private void validateReservableItems(List<OrderCreateRequest.Item> items, Map<Long, ProductEntity> lockedProducts) {
        Map<Long, Integer> requestedQuantities = aggregateRequestedQuantities(items);
        Map<Long, Integer> reservedQuantities = getReservedQuantities(new ArrayList<>(requestedQuantities.keySet()));

        for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
            ProductEntity product = lockedProducts.get(entry.getKey());
            validateItem(product, OrderCreateRequest.Item.builder()
                    .productId(entry.getKey())
                    .quantity(entry.getValue())
                    .unitPrice(product.getPrice())
                    .build());

            int reserved = reservedQuantities.getOrDefault(entry.getKey(), 0);
            int available = Math.max(0, product.getStock() - reserved);
            if (entry.getValue() > available) {
                throw new BadRequestException("Stock insuficiente para " + product.getName());
            }
        }
    }

    private void commitReservedInventory(OrderEntity order) {
        Map<Long, Integer> quantities = aggregateOrderItemQuantities(order.getItems());
        Map<Long, ProductEntity> lockedProducts = lockProducts(new ArrayList<>(quantities.keySet()));

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            ProductEntity product = lockedProducts.get(entry.getKey());
            int quantity = entry.getValue();
            if (product.getStock() < quantity) {
                throw new BadRequestException("Stock fisico insuficiente para " + product.getName());
            }

            int stockBefore = product.getStock();
            int stockAfter = product.getStock() - quantity;
            product.setStock(stockAfter);
            if (stockAfter <= 0) {
                product.setStatus(ProductStatus.SOLD_OUT);
            } else if (product.getStatus() == ProductStatus.SOLD_OUT) {
                product.setStatus(ProductStatus.AVAILABLE);
            }
            stockMovementRepository.save(buildStockMovement(
                    order,
                    product,
                    MovementType.COMMITTED,
                    quantity,
                    stockBefore,
                    stockAfter,
                    DEFAULT_COMMIT_REASON));
        }

        productRepository.saveAll(lockedProducts.values());
    }

    private void releaseInventory(OrderEntity order, String reason) {
        if (order.getInventoryStatus() == InventoryStatus.RELEASED) {
            return;
        }
        String movementReason = reason != null && !reason.isBlank() ? reason : DEFAULT_CANCEL_REASON;
        Map<Long, Integer> quantities = aggregateOrderItemQuantities(order.getItems());
        Map<Long, ProductEntity> lockedProducts = lockProducts(new ArrayList<>(quantities.keySet()));
        boolean inventoryChanged = false;

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            ProductEntity product = lockedProducts.get(entry.getKey());
            int quantity = entry.getValue();
            int stockBefore = product.getStock();
            int stockAfter = stockBefore;

            if (order.getInventoryStatus() == InventoryStatus.COMMITTED) {
                stockAfter = stockBefore + quantity;
                product.setStock(stockAfter);
                if (stockAfter > 0 && product.getStatus() == ProductStatus.SOLD_OUT) {
                    product.setStatus(ProductStatus.AVAILABLE);
                }
                inventoryChanged = true;
            }

            stockMovementRepository.save(buildStockMovement(
                    order,
                    product,
                    MovementType.RELEASED,
                    quantity,
                    stockBefore,
                    stockAfter,
                    movementReason));
        }

        if (inventoryChanged) {
            productRepository.saveAll(lockedProducts.values());
        }
        order.setInventoryStatus(InventoryStatus.RELEASED);
    }

    private void restoreInventoryAfterRefund(OrderEntity order, String reason) {
        if (order.getInventoryStatus() != InventoryStatus.COMMITTED) {
            order.setInventoryStatus(InventoryStatus.RELEASED);
            return;
        }

        String movementReason = reason != null && !reason.isBlank() ? reason : DEFAULT_REFUND_REASON;
        Map<Long, Integer> quantities = aggregateOrderItemQuantities(order.getItems());
        Map<Long, ProductEntity> lockedProducts = lockProducts(new ArrayList<>(quantities.keySet()));

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            ProductEntity product = lockedProducts.get(entry.getKey());
            int quantity = entry.getValue();
            int stockBefore = product.getStock();
            int stockAfter = stockBefore + quantity;
            product.setStock(stockAfter);
            if (stockAfter > 0 && product.getStatus() == ProductStatus.SOLD_OUT) {
                product.setStatus(ProductStatus.AVAILABLE);
            }
            stockMovementRepository.save(buildStockMovement(
                    order,
                    product,
                    MovementType.REFUND,
                    quantity,
                    stockBefore,
                    stockAfter,
                    movementReason));
        }

        productRepository.saveAll(lockedProducts.values());
        order.setInventoryStatus(InventoryStatus.RELEASED);
    }

    private Map<Long, ProductEntity> lockProducts(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        List<Long> sortedIds = productIds.stream()
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        List<ProductEntity> products = productRepository.findAllByIdInOrderByIdForUpdate(sortedIds);
        if (products.size() != sortedIds.size()) {
            List<Long> foundIds = products.stream().map(ProductEntity::getId).toList();
            Long missingId = sortedIds.stream().filter(id -> !foundIds.contains(id)).findFirst().orElse(null);
            throw new NotFoundException("Producto no encontrado: " + missingId);
        }

        Map<Long, ProductEntity> byId = new LinkedHashMap<>();
        for (ProductEntity product : products) {
            byId.put(product.getId(), product);
        }
        return byId;
    }

    private List<Long> extractProductIds(List<OrderCreateRequest.Item> items) {
        return items.stream()
                .map(OrderCreateRequest.Item::getProductId)
                .toList();
    }

    private Map<Long, Integer> aggregateRequestedQuantities(List<OrderCreateRequest.Item> items) {
        Map<Long, Integer> quantities = new HashMap<>();
        for (OrderCreateRequest.Item item : items) {
            quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return quantities;
    }

    private Map<Long, Integer> aggregateOrderItemQuantities(List<OrderItemEntity> items) {
        Map<Long, Integer> quantities = new HashMap<>();
        for (OrderItemEntity item : items) {
            quantities.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
        }
        return quantities;
    }

    private Map<Long, Integer> getReservedQuantities(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Integer> quantities = new HashMap<>();
        for (Object[] row : orderItemRepository.sumReservedQuantitiesByProductIds(productIds)) {
            Long productId = ((Number) row[0]).longValue();
            Integer quantity = row[1] != null ? ((Number) row[1]).intValue() : 0;
            quantities.put(productId, quantity);
        }
        return quantities;
    }

    private void ensureMutable(OrderEntity order) {
        if (order.getTransactionStatus() == TransactionStatus.CANCELLED) {
            throw new BadRequestException("La orden esta cancelada");
        }
        if (order.getTransactionStatus() == TransactionStatus.REFUNDED) {
            throw new BadRequestException("La orden ya fue reembolsada");
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("La orden ya no admite cambios operativos");
        }
    }

    private void recordReservationMovements(OrderEntity order, Map<Long, ProductEntity> lockedProducts) {
        for (Map.Entry<Long, Integer> entry : aggregateOrderItemQuantities(order.getItems()).entrySet()) {
            ProductEntity product = lockedProducts.get(entry.getKey());
            int physicalStock = product.getStock();
            stockMovementRepository.save(buildStockMovement(
                    order,
                    product,
                    MovementType.RESERVED,
                    entry.getValue(),
                    physicalStock,
                    physicalStock,
                    DEFAULT_RESERVATION_REASON));
        }
    }

    private StockMovementEntity buildStockMovement(OrderEntity order,
                                                   ProductEntity product,
                                                   MovementType movementType,
                                                   int quantity,
                                                   int stockBefore,
                                                   int stockAfter,
                                                   String reason) {
        StockMovementEntity movement = new StockMovementEntity();
        movement.setProduct(product);
        movement.setType(movementType);
        movement.setQuantity(Math.abs(quantity));
        movement.setStockBefore(stockBefore);
        movement.setStockAfter(stockAfter);
        movement.setReason(reason);
        movement.setUser(resolveMovementUser(order));
        movement.setReferenceType(ORDER_REFERENCE_TYPE);
        movement.setReferenceId(order.getId());
        return movement;
    }

    private UserEntity resolveMovementUser(OrderEntity order) {
        if (order.getUser() != null) {
            return order.getUser();
        }
        return userRepository.findByEmail("admin@ucacue.com")
                .orElseThrow(() -> new NotFoundException("No existe un usuario para auditar movimientos"));
    }

    private void publishOrderEvent(String type, OrderEntity order) {
        org.hibernate.Hibernate.initialize(order.getItems());
        var orderResponse = OrderMapper.toResponse(order);
        Integer estimatedMinutes = estimateMinutesFromOrder(order);
        realtimeGateway.sales(Map.of(
                "type", type,
                "order", orderResponse));

        var notifyPayload = new HashMap<String, Object>();
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
        realtimeGateway.orders(type, Map.of(
                "type", "order." + type,
                "order", orderResponse));
    }

    private void publishPaymentEvent(OrderEntity order) {
        var payload = Map.of(
                "type", "ORDER_PAYMENT_UPDATED",
                "order", OrderMapper.toResponse(order));
        realtimeGateway.sales(payload);
        realtimeGateway.notifyChannel(payload);
    }

    private void publishAvailabilityChange() {
        try {
            realtimeGateway.products("PRODUCTS_CHANGED");
            realtimeGateway.inventory("INVENTORY_CHANGED");
        } catch (Exception ex) {
            log.debug("Unable to publish inventory availability update: {}", ex.getMessage());
        }
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
}
