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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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
@Transactional(readOnly = true)
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
    private final AuditService auditService;

    @Transactional
    @CacheEvict(cacheNames = {"products-list", "products-public", "products-low-stock", "dashboard-snapshots", "sales-reports"}, allEntries = true)
    public OrderResponse createOrder(OrderCreateRequest request, Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AccessDeniedException("Usuario no autenticado");
        }
        UserEntity user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> new AccessDeniedException("Usuario no autenticado"));

        if (request.getItems().isEmpty()) {
            throw new BadRequestException("El pedido debe contener productos");
        }

        boolean canApplyManualDiscount = isStaff(authentication);
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        Map<Long, ProductEntity> lockedProducts = lockProducts(tenantId, extractProductIds(request.getItems()));
        validateReservableItems(request.getItems(), lockedProducts);

        OrderEntity order = new OrderEntity();
        order.setTenantId(tenantId);
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
        applyCustomerData(order, request);

        List<TaxComputationService.TaxableLine> taxableLines = new ArrayList<>();
        for (OrderCreateRequest.Item item : request.getItems()) {
            ProductEntity product = lockedProducts.get(item.getProductId());
            validateItem(product, item);

            int qty = item.getQuantity();
            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setTenantId(tenantId);
            orderItem.setProduct(product);
            orderItem.setQuantity(qty);
            BigDecimal unitPrice = product.getPrice();
            orderItem.setUnitPrice(unitPrice);

            BigDecimal grossLineTotal = unitPrice
                    .multiply(BigDecimal.valueOf(qty))
                    .setScale(2, RoundingMode.HALF_UP);
            validateDiscountAuthorization(item.getDiscountAmount(), canApplyManualDiscount);
            BigDecimal discountAmount = sanitizeDiscount(
                    canApplyManualDiscount ? item.getDiscountAmount() : null,
                    grossLineTotal);
            BigDecimal lineTotal = grossLineTotal.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
            orderItem.setLineTotal(lineTotal);

            order.addItem(orderItem);
            taxableLines.add(new TaxComputationService.TaxableLine(
                    product.getId(),
                    qty,
                    lineTotal.divide(BigDecimal.valueOf(qty), 6, RoundingMode.HALF_UP)));
        }
        TaxSummary taxSummary = taxComputationService.calculate(
                tenantContextResolver.resolveCurrent().getId(),
                taxableLines);
        order.setSubtotal(taxSummary.subtotal());
        order.setTax(taxSummary.taxAmount());
        order.setTotal(taxSummary.total());

        OrderEntity saved = orderRepository.save(order);
        orderItemRepository.saveAll(saved.getItems());
        recordReservationMovements(saved, lockedProducts);

        log.info("Order {} created for {} with {} items and reserved inventory", saved.getId(), user.getEmail(),
                saved.getItems().size());
        auditService.logAction(user.getEmail(), "ORDER_CREATE", "Order", saved.getId(),
                "items=" + saved.getItems().size() + " total=" + saved.getTotal());
        publishAvailabilityChange();
        publishOrderEvent("created", saved);
        dashboardService.publishSnapshotAsync();
        return OrderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> list(OrderStatus status, TransactionStatus transactionStatus, Pageable pageable) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        Page<OrderEntity> orders;
        if (status != null && transactionStatus != null) {
            orders = orderRepository.findByTenantIdAndStatusAndTransactionStatus(tenantId, status, transactionStatus, pageable);
        } else if (transactionStatus != null) {
            orders = orderRepository.findByTenantIdAndTransactionStatus(tenantId, transactionStatus, pageable);
        } else if (status != null) {
            orders = orderRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else {
            orders = orderRepository.findByTenantId(tenantId, pageable);
        }
        if (orders.isEmpty()) {
            return orders.map(OrderMapper::toResponse);
        }

        List<Long> orderIds = orders.getContent().stream()
                .map(OrderEntity::getId)
                .toList();
        Map<Long, OrderEntity> detailedById = new HashMap<>();
        for (OrderEntity order : orderRepository.findDetailedByTenantIdAndIdIn(tenantId, orderIds)) {
            detailedById.put(order.getId(), order);
        }
        List<OrderResponse> content = orderIds.stream()
                .map(detailedById::get)
                .filter(java.util.Objects::nonNull)
                .map(OrderMapper::toResponse)
                .toList();
        return new PageImpl<>(content, pageable, orders.getTotalElements());
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id, Authentication authentication) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        OrderEntity order = orderRepository.findDetailedByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado"));
        ensureTenantOwnership(order);
        ensureCanRead(order, authentication);
        return OrderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> previewTotals(List<OrderCreateRequest.Item> items) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        Map<Long, ProductEntity> productsById = new HashMap<>();
        for (OrderCreateRequest.Item item : items) {
            ProductEntity product = productsById.computeIfAbsent(
                    item.getProductId(),
                    productId -> loadProductForTenant(tenantId, productId));
            validateItem(product, item);
        }
        TaxSummary taxSummary = taxComputationService.calculate(
                tenantId,
                items.stream()
                        .map(item -> new TaxComputationService.TaxableLine(
                                item.getProductId(),
                                item.getQuantity(),
                                productsById.get(item.getProductId()).getPrice()))
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
    @CacheEvict(cacheNames = {"products-list", "products-public", "products-low-stock", "dashboard-snapshots", "sales-reports"}, allEntries = true)
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
        auditService.logAction(currentUserEmail(), "ORDER_CANCEL", "Order", saved.getId(),
                "reason=" + (reason != null ? reason : DEFAULT_CANCEL_REASON));
        publishOrderEvent("cancelled", saved);
        publishPaymentEvent(saved);
        dashboardService.publishSnapshotAsync();
        return OrderMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {"products-list", "products-public", "products-low-stock", "dashboard-snapshots", "sales-reports"}, allEntries = true)
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
        auditService.logAction(currentUserEmail(), "ORDER_REFUND", "Order", saved.getId(),
                "total=" + saved.getTotal() + " reason=" + (reason != null ? reason : DEFAULT_REFUND_REASON));
        publishAvailabilityChange();
        publishOrderEvent("refunded", saved);
        publishPaymentEvent(saved);
        dashboardService.publishSnapshotAsync();
        return OrderMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {"products-list", "products-public", "products-low-stock", "dashboard-snapshots", "sales-reports"}, allEntries = true)
    public OrderResponse pay(Long orderId, String method, String reference) {
        return pay(orderId, method, reference, null);
    }

    @Transactional
    @CacheEvict(cacheNames = {"products-list", "products-public", "products-low-stock", "dashboard-snapshots", "sales-reports"}, allEntries = true)
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
    @CacheEvict(cacheNames = {"products-list", "products-public", "products-low-stock", "dashboard-snapshots", "sales-reports"}, allEntries = true)
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

    private void ensureTenantOwnership(OrderEntity order) {
        Long current = tenantContextResolver.resolveCurrent().getId();
        if (order.getTenantId() == null || !order.getTenantId().equals(current)) {
            throw new NotFoundException("Pedido no encontrado");
        }
    }

    private OrderEntity requireOrder(Long id) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return orderRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado"));
    }

    private OrderEntity requireOrderForUpdate(Long id) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        OrderEntity order = orderRepository.findByTenantIdAndIdForUpdate(tenantId, id)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado"));
        return order;
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
        auditService.logAction(currentUserEmail(), "ORDER_PAY", "Order", saved.getId(),
                "method=" + saved.getPaymentMethod() + " total=" + saved.getTotal());
        if (inventoryCommitted) {
            publishAvailabilityChange();
        }
        publishPaymentEvent(saved);
        dashboardService.publishSnapshotAsync();
        return OrderMapper.toResponse(saved);
    }

    private String currentUserEmail() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            return auth != null ? auth.getName() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private void applyCustomerData(OrderEntity order, OrderCreateRequest request) {
        OrderEntity.DocumentType documentType = request.getDocumentType() == null
                ? OrderEntity.DocumentType.NOTA_VENTA
                : OrderEntity.DocumentType.valueOf(request.getDocumentType().name());
        order.setDocumentType(documentType);
        order.setCustomerName(trimToNull(request.getCustomerName()));
        order.setCustomerEmail(trimToNull(request.getCustomerEmail()));
        String identification = digitsOnly(request.getCustomerIdentification());
        order.setCustomerIdentification(identification.isBlank() ? null : identification);
        order.setCustomerIdentificationType(resolveIdentificationType(order.getCustomerIdentification()));
        order.setCustomerAddress(trimToNull(request.getCustomerAddress()));
        order.setCustomerPhone(trimToNull(request.getCustomerPhone()));

        if (documentType == OrderEntity.DocumentType.FACTURA) {
            if (order.getCustomerIdentification() == null) {
                throw new BadRequestException("La factura electronica requiere identificacion del cliente");
            }
            int idLength = order.getCustomerIdentification().length();
            if (idLength != 10 && idLength != 13) {
                throw new BadRequestException("La identificacion debe tener 10 (cedula) o 13 (RUC) digitos");
            }
            if (order.getCustomerName() == null) {
                throw new BadRequestException("La factura electronica requiere razon social del cliente");
            }
            if (order.getCustomerAddress() == null) {
                throw new BadRequestException("La factura electronica requiere la direccion del cliente");
            }
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String resolveIdentificationType(String identification) {
        if (identification == null || identification.isBlank()) {
            return "07";
        }
        return switch (identification.length()) {
            case 13 -> "04";
            case 10 -> "05";
            default -> "06";
        };
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
    }

    private void validateReservableItems(List<OrderCreateRequest.Item> items, Map<Long, ProductEntity> lockedProducts) {
        Map<Long, Integer> requestedQuantities = aggregateRequestedQuantities(items);
        Map<Long, Integer> reservedQuantities = getReservedQuantities(new ArrayList<>(requestedQuantities.keySet()));

        for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
            ProductEntity product = lockedProducts.get(entry.getKey());
            validateItem(product, OrderCreateRequest.Item.builder()
                    .productId(entry.getKey())
                    .quantity(entry.getValue())
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
        Map<Long, ProductEntity> lockedProducts = lockProducts(order.getTenantId(), new ArrayList<>(quantities.keySet()));

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
        Map<Long, ProductEntity> lockedProducts = lockProducts(order.getTenantId(), new ArrayList<>(quantities.keySet()));
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
        Map<Long, ProductEntity> lockedProducts = lockProducts(order.getTenantId(), new ArrayList<>(quantities.keySet()));

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

    private Map<Long, ProductEntity> lockProducts(Long tenantId, List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        List<Long> sortedIds = productIds.stream()
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        List<ProductEntity> products = productRepository.findAllByTenantIdAndIdInOrderByIdForUpdate(tenantId, sortedIds);
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

    private ProductEntity loadProductForTenant(Long tenantId, Long productId) {
        return productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado: " + productId));
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
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        for (Object[] row : orderItemRepository.sumReservedQuantitiesByProductIdsAndTenantId(tenantId, productIds)) {
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

    private void ensureCanRead(OrderEntity order, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuario no autenticado");
        }
        if (isStaff(authentication)) {
            return;
        }
        String requester = authentication.getName();
        String owner = order.getUser() != null ? order.getUser().getEmail() : null;
        if (owner != null && owner.equalsIgnoreCase(requester)) {
            return;
        }
        throw new AccessDeniedException("No puede acceder a esta orden");
    }

    private boolean isStaff(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_CASHIER".equals(authority.getAuthority()));
    }

    private void validateDiscountAuthorization(BigDecimal requestedDiscount, boolean canApplyManualDiscount) {
        if (!canApplyManualDiscount
                && requestedDiscount != null
                && requestedDiscount.compareTo(BigDecimal.ZERO) > 0) {
            throw new AccessDeniedException("No puede aplicar descuentos manuales");
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
        if (order.getTenantId() != null) {
            movement.setTenantId(order.getTenantId());
        }
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
        throw new NotFoundException("La orden no tiene un usuario asociado para auditar movimientos");
    }

    private void publishOrderEvent(String type, OrderEntity order) {
        try {
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
        } catch (Exception ex) {
            log.warn("Unable to publish order event {}", type, ex);
        }
    }

    private void publishPaymentEvent(OrderEntity order) {
        try {
            var payload = Map.of(
                    "type", "ORDER_PAYMENT_UPDATED",
                    "order", OrderMapper.toResponse(order));
            realtimeGateway.sales(payload);
            realtimeGateway.notifyChannel(payload);
        } catch (Exception ex) {
            log.warn("Unable to publish payment event for order {}", order.getId(), ex);
        }
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
