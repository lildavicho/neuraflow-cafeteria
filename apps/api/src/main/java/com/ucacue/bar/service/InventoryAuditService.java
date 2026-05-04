package com.ucacue.bar.service;

import com.ucacue.bar.entity.InventoryAlertEntity;
import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.entity.StockMovementEntity;
import com.ucacue.bar.entity.StockMovementEntity.MovementType;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.erp.shared.application.TabularExportService;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.repository.InventoryAlertRepository;
import com.ucacue.bar.repository.OrderItemRepository;
import com.ucacue.bar.repository.ProductRepository;
import com.ucacue.bar.repository.StockMovementRepository;
import com.ucacue.bar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryAuditService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryAlertRepository inventoryAlertRepository;
    private final OrderItemRepository orderItemRepository;
    private final TenantContextResolver tenantContextResolver;
    private final TabularExportService tabularExportService;

    @Transactional(readOnly = true)
    public List<StockMovementView> listRecent() {
        return listRecent(50);
    }

    @Transactional(readOnly = true)
    public List<StockMovementView> listRecent(int size) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        int safeSize = Math.min(Math.max(size, 1), 200);
        return stockMovementRepository.findByTenantId(tenantId,
                        PageRequest.of(0, safeSize, org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<StockMovementView> listFiltered(Long productId, String type, String warehouse,
                                                 LocalDateTime from, LocalDateTime to,
                                                 int page, int size) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        MovementType movementType = parseMovementType(type);
        Pageable pageable = PageRequest.of(page, size);
        return stockMovementRepository.findFilteredByTenantId(tenantId, productId, movementType, warehouse, from, to, pageable)
                .map(this::toView);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementView> listByProduct(Long productId, int page, int size) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return stockMovementRepository.findByTenantIdAndProductIdOrderByCreatedAtDesc(tenantId, productId, PageRequest.of(page, size))
                .map(this::toView);
    }

    @Transactional
    public StockMovementView registerMovement(KardexMovementRequest request, Long userId) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        if (request.productId() == null) {
            throw new BadRequestException("El producto es obligatorio");
        }
        ProductEntity product = productRepository.findByTenantIdAndId(tenantId, request.productId())
                .orElseThrow(() -> new NotFoundException("Producto no encontrado: " + request.productId()));

        UserEntity user = userRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + userId));
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new BadRequestException("La cantidad debe ser mayor a cero");
        }

        MovementType movementType = parseMovementType(request.type());
        if (movementType == null) {
            throw new BadRequestException("El tipo de movimiento es obligatorio");
        }
        int currentStock = product.getStock();
        int newStock;

        switch (movementType) {
            case IN, REFUND, RELEASED -> {
                newStock = currentStock + request.quantity();
            }
            case OUT, RESERVED, COMMITTED -> {
                if (currentStock < request.quantity()) {
                    throw new BadRequestException("Stock insuficiente. Disponible: " + currentStock
                            + ", Solicitado: " + request.quantity());
                }
                newStock = currentStock - request.quantity();
            }
            case ADJUST -> {
                newStock = request.quantity();
            }
            default -> throw new BadRequestException("Tipo de movimiento inválido: " + request.type());
        }

        StockMovementEntity movement = new StockMovementEntity();
        movement.setTenantId(tenantId);
        movement.setProduct(product);
        movement.setType(movementType);
        movement.setQuantity(request.quantity());
        movement.setStockBefore(currentStock);
        movement.setStockAfter(newStock);
        movement.setReason(request.reason());
        movement.setUser(user);
        movement.setReferenceType(request.referenceType());
        movement.setReferenceId(request.referenceId());
        movement.setBatchCode(request.batchCode());
        movement.setSerialNumber(request.serialNumber());
        movement.setUnitCost(request.unitCost());
        movement.setTotalCost(request.unitCost() != null
                ? request.unitCost().multiply(BigDecimal.valueOf(request.quantity()))
                : null);
        movement.setWarehouse(request.warehouse() != null ? request.warehouse() : "PRINCIPAL");
        movement.setExpiryDate(request.expiryDate());
        movement.setDocumentNumber(request.documentNumber());

        product.setStock(newStock);
        productRepository.save(product);

        StockMovementEntity saved = stockMovementRepository.save(movement);
        log.info("KARDEX: {} {} uds de '{}' por {}. Stock: {} -> {}",
                movementType, request.quantity(), product.getName(),
                user.getEmail(), currentStock, newStock);

        checkStockAlerts(product, newStock);

        return toView(saved);
    }

    @Transactional(readOnly = true)
    public List<String> listWarehouses() {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return stockMovementRepository.findDistinctWarehousesByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public Page<AlertView> listActiveAlerts(int page, int size) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return inventoryAlertRepository.findByTenantIdAndAcknowledgedFalseOrderByCreatedAtDesc(tenantId, PageRequest.of(page, size))
                .map(a -> new AlertView(a.getId(), a.getProduct().getId(), a.getProduct().getName(),
                        a.getAlertType(), a.getMessage(), a.getSeverity(), a.getCreatedAt()));
    }

    @Transactional
    public void acknowledgeAlert(Long alertId, Long userId) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        InventoryAlertEntity alert = inventoryAlertRepository.findByTenantIdAndId(tenantId, alertId)
                .orElseThrow(() -> new NotFoundException("Alerta no encontrada: " + alertId));
        alert.setAcknowledged(true);
        alert.setAcknowledgedBy(userId);
        alert.setAcknowledgedAt(LocalDateTime.now());
        inventoryAlertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public KardexSummary getSummary() {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        List<ProductEntity> products = productRepository.findByTenantId(tenantId, Pageable.unpaged()).getContent();
        long totalProducts = products.size();
        int totalStock = products.stream().mapToInt(ProductEntity::getStock).sum();
        long lowStockCount = products.stream()
                .filter(p -> p.getStock() <= p.getReorderPoint())
                .count();
        long outOfStockCount = products.stream()
                .filter(p -> p.getStock() <= 0)
                .count();
        BigDecimal totalValuation = products.stream()
                .filter(p -> p.getPurchasePrice() != null)
                .map(p -> p.getPurchasePrice().multiply(BigDecimal.valueOf(p.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pendingAlerts = inventoryAlertRepository.countByTenantIdAndAcknowledgedFalse(tenantId);

        return new KardexSummary(totalProducts, totalStock, lowStockCount, outOfStockCount,
                totalValuation, pendingAlerts);
    }

    @Transactional(readOnly = true)
    public TabularExportService.ExportDocument exportProducts(String format, String search, String category) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        String normalizedSearch = search != null ? search.trim().toLowerCase() : "";
        String normalizedCategory = category != null ? category.trim().toLowerCase() : "";
        List<ProductEntity> products = productRepository.findByTenantId(tenantId, Pageable.unpaged()).getContent();
        List<Long> productIds = products.stream().map(ProductEntity::getId).toList();
        java.util.Map<Long, Integer> reservedQuantities = productIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : orderItemRepository
                .sumReservedQuantitiesByProductIdsAndTenantId(tenantId, productIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()));

        List<List<?>> rows = new java.util.ArrayList<>();
        for (ProductEntity product : products) {
            String categoryName = product.getCategory() != null ? product.getCategory().getName() : null;
            if (!normalizedSearch.isBlank()
                    && !containsIgnoreCase(product.getName(), normalizedSearch)
                    && !containsIgnoreCase(product.getCode(), normalizedSearch)
                    && !containsIgnoreCase(product.getWarehouse(), normalizedSearch)
                    && !containsIgnoreCase(categoryName, normalizedSearch)) {
                continue;
            }
            if (!normalizedCategory.isBlank() && !containsIgnoreCase(categoryName, normalizedCategory)) {
                continue;
            }
            int reserved = reservedQuantities.getOrDefault(product.getId(), 0);
            rows.add(java.util.Arrays.asList(
                    product.getId(),
                    product.getCode(),
                    product.getName(),
                    categoryName,
                    product.getStatus() != null ? product.getStatus().name() : null,
                    product.getWarehouse(),
                    product.getStock(),
                    reserved,
                    Math.max(0, product.getStock() - reserved),
                    product.getMinStock(),
                    product.getReorderPoint(),
                    product.getMaxStock(),
                    product.getPrice(),
                    product.getPurchasePrice(),
                    product.getCreatedAt(),
                    product.getUpdatedAt()));
        }

        return tabularExportService.export(
                "Inventario",
                List.of("ID", "Codigo", "Producto", "Categoria", "Estado", "Bodega", "Stock", "Reservado", "Disponible", "Minimo", "Reorden", "Maximo", "Precio", "Costo", "Creado", "Actualizado"),
                rows,
                format);
    }

    private void checkStockAlerts(ProductEntity product, int newStock) {
        if (newStock <= 0) {
            createAlertIfNeeded(product, "OUT_OF_STOCK",
                    "Producto '" + product.getName() + "' agotado (stock: 0)", "CRITICAL");
        } else if (newStock <= product.getReorderPoint()) {
            createAlertIfNeeded(product, "LOW_STOCK",
                    "Producto '" + product.getName() + "' bajo mínimo (stock: " + newStock
                            + ", reorden: " + product.getReorderPoint() + ")", "WARNING");
        } else if (newStock >= product.getMaxStock()) {
            createAlertIfNeeded(product, "OVERSTOCK",
                    "Producto '" + product.getName() + "' excede máximo (stock: " + newStock
                            + ", máximo: " + product.getMaxStock() + ")", "INFO");
        }
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private void createAlertIfNeeded(ProductEntity product, String alertType, String message, String severity) {
        Long tenantId = product.getTenantId();
        if (tenantId == null) {
            throw new NotFoundException("Producto no encontrado: " + product.getId());
        }
        List<InventoryAlertEntity> existing = inventoryAlertRepository
                .findByTenantIdAndProductIdAndAcknowledgedFalse(tenantId, product.getId());
        boolean alreadyExists = existing.stream().anyMatch(a -> a.getAlertType().equals(alertType));
        if (!alreadyExists) {
            inventoryAlertRepository.save(InventoryAlertEntity.builder()
                    .tenantId(tenantId)
                    .product(product)
                    .alertType(alertType)
                    .message(message)
                    .severity(severity)
                    .build());
        }
    }

    private StockMovementView toView(StockMovementEntity entity) {
        return new StockMovementView(
                entity.getId(),
                entity.getProduct().getId(),
                entity.getProduct().getName(),
                entity.getType().name(),
                entity.getQuantity(),
                entity.getStockBefore(),
                entity.getStockAfter(),
                entity.getReason(),
                entity.getReferenceType(),
                entity.getReferenceId(),
                entity.getUser() != null ? entity.getUser().getName() : null,
                entity.getBatchCode(),
                entity.getSerialNumber(),
                entity.getUnitCost(),
                entity.getTotalCost(),
                entity.getWarehouse(),
                entity.getExpiryDate(),
                entity.getDocumentNumber(),
                entity.getCreatedAt());
    }

    private MovementType parseMovementType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return MovementType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Tipo de movimiento inválido: " + type);
        }
    }

    public record StockMovementView(
            Long id, Long productId, String productName, String movementType,
            Integer quantity, Integer stockBefore, Integer stockAfter,
            String reason, String referenceType, Long referenceId, String userName,
            String batchCode, String serialNumber, BigDecimal unitCost, BigDecimal totalCost,
            String warehouse, LocalDate expiryDate, String documentNumber,
            LocalDateTime createdAt) {
    }

    public record KardexMovementRequest(
            Long productId, String type, Integer quantity, String reason,
            String referenceType, Long referenceId,
            String batchCode, String serialNumber, BigDecimal unitCost,
            String warehouse, LocalDate expiryDate, String documentNumber) {
    }

    public record AlertView(
            Long id, Long productId, String productName,
            String alertType, String message, String severity,
            LocalDateTime createdAt) {
    }

    public record KardexSummary(
            long totalProducts, int totalStock, long lowStockCount, long outOfStockCount,
            BigDecimal totalValuation, long pendingAlerts) {
    }
}
