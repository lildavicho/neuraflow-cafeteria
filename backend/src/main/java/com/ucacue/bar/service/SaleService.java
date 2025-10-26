package com.ucacue.bar.service;

import com.ucacue.bar.dto.CreateSaleRequest;
import com.ucacue.bar.dto.SaleDTO;
import com.ucacue.bar.entity.*;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.exception.UnauthorizedException;
import com.ucacue.bar.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SaleService {
    
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StockMovementRepository stockMovementRepository;
    private final EmailService emailService;
    
    private static final BigDecimal IVA_RATE = new BigDecimal("0.15");
    
    @Transactional
    public SaleDTO createSale(CreateSaleRequest request, String userEmail) {
        // Get user
        UserEntity user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        
        // Validate products and calculate totals
        BigDecimal subtotal = BigDecimal.ZERO;
        List<SaleItemEntity> saleItems = new ArrayList<>();
        
        for (CreateSaleRequest.SaleItemRequest itemRequest : request.getItems()) {
            ProductEntity product = productRepository.findById(itemRequest.getProductId())
                .orElseThrow(() -> new NotFoundException("Producto no encontrado: " + itemRequest.getProductId()));
            
            // Check stock
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new BadRequestException("Stock insuficiente para " + product.getName());
            }
            
            // Create sale item
            SaleItemEntity saleItem = new SaleItemEntity();
            saleItem.setProduct(product);
            saleItem.setQuantity(itemRequest.getQuantity());
            saleItem.setUnitPrice(itemRequest.getUnitPrice());
            
            BigDecimal itemSubtotal = itemRequest.getUnitPrice()
                .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            saleItem.setSubtotal(itemSubtotal);
            saleItem.setSubtotalDiscount(BigDecimal.ZERO);
            
            saleItems.add(saleItem);
            subtotal = subtotal.add(itemSubtotal);
            
            // Update product stock
            product.setStock(product.getStock() - itemRequest.getQuantity());
            if (product.getStock() <= 0) {
                product.setStatus(ProductEntity.ProductStatus.SOLD_OUT);
            }
            productRepository.save(product);
            
            // Record stock movement
            StockMovementEntity movement = new StockMovementEntity();
            movement.setProduct(product);
            movement.setType(StockMovementEntity.MovementType.OUT);
            movement.setQuantity(itemRequest.getQuantity());
            movement.setStockBefore(product.getStock() + itemRequest.getQuantity());
            movement.setStockAfter(product.getStock());
            movement.setReason("Venta");
            movement.setUser(user);
            movement.setReferenceType("SALE");
            stockMovementRepository.save(movement);
            
            // Check for low stock alert
            if (product.getStock() <= product.getMinStock()) {
                emailService.sendLowStockAlert(
                    "admin@ucacue.edu.ec",
                    product.getName(),
                    product.getStock()
                );
            }
        }
        
        // Calculate IVA and total
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal subtotalAfterDiscount = subtotal.subtract(discount);
        BigDecimal iva = subtotalAfterDiscount.multiply(IVA_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotalAfterDiscount.add(iva);
        
        // Create sale
        SaleEntity sale = new SaleEntity();
        sale.setInvoiceNumber(generateInvoiceNumber());
        sale.setUser(user);
        sale.setSubtotal(subtotal);
        sale.setDiscount(discount);
        sale.setIva(iva);
        sale.setTotal(total);
        sale.setPaymentMethod(SaleEntity.PaymentMethod.valueOf(request.getPaymentMethod()));
        sale.setOrderType(SaleEntity.OrderType.valueOf(request.getOrderType()));
        sale.setStatus(SaleEntity.SaleStatus.PAID);
        sale.setNotes(request.getNotes());
        sale.setPaymentReference(request.getPaymentReference());
        
        sale = saleRepository.save(sale);
        
        // Set sale reference in items and save
        for (SaleItemEntity item : saleItems) {
            item.setSale(sale);
        }
        sale.setItems(saleItems);
        sale = saleRepository.save(sale);
        
        // Send order confirmation email
        emailService.sendOrderConfirmation(
            user.getEmail(),
            sale.getInvoiceNumber(),
            total.toString()
        );
        
        log.info("Sale created: {} for user {}", sale.getInvoiceNumber(), user.getEmail());
        
        return convertToDTO(sale);
    }
    
    public SaleDTO getSaleById(Long id, Authentication authentication) {
        SaleEntity sale = saleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Venta no encontrada"));
        
        // Check permission
        UserEntity currentUser = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        
        if (currentUser.getRole() != UserEntity.UserRole.ADMIN && 
            !sale.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("No tienes permiso para ver esta venta");
        }
        
        return convertToDTO(sale);
    }
    
    public Page<SaleDTO> getSales(LocalDateTime from, LocalDateTime to, 
                                  String status, String paymentMethod, Pageable pageable) {
        Page<SaleEntity> sales;
        
        if (from != null && to != null) {
            sales = saleRepository.findByDateRange(from, to, pageable);
        } else if (status != null) {
            sales = saleRepository.findByStatus(SaleEntity.SaleStatus.valueOf(status), pageable);
        } else {
            sales = saleRepository.findAll(pageable);
        }
        
        return sales.map(this::convertToDTO);
    }
    
    public Page<SaleDTO> getUserSales(String userEmail, Pageable pageable) {
        UserEntity user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        
        Page<SaleEntity> sales = saleRepository.findByUserId(user.getId(), pageable);
        return sales.map(this::convertToDTO);
    }
    
    public Map<String, Object> getSalesSummary(String period) {
        Map<String, Object> summary = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;
        
        switch (period.toLowerCase()) {
            case "weekly":
                startDate = now.minusWeeks(1);
                break;
            case "monthly":
                startDate = now.minusMonths(1);
                break;
            default:
                startDate = now.minusDays(1);
        }
        
        // Get total sales
        BigDecimal totalSales = saleRepository.calculateTotalSales(startDate, now);
        Long totalCount = saleRepository.countSalesInPeriod(startDate, now);
        
        // Get top products
        List<Object[]> topProducts = saleRepository.findTopSellingProducts(startDate, now, 10);
        
        summary.put("period", period);
        summary.put("startDate", startDate);
        summary.put("endDate", now);
        summary.put("totalSales", totalSales != null ? totalSales : BigDecimal.ZERO);
        summary.put("totalCount", totalCount != null ? totalCount : 0);
        summary.put("averageSale", totalCount > 0 ? 
            totalSales.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        summary.put("topProducts", topProducts);
        
        return summary;
    }
    
    @Transactional
    public SaleDTO updateSaleStatus(Long id, String status) {
        SaleEntity sale = saleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Venta no encontrada"));
        
        sale.setStatus(SaleEntity.SaleStatus.valueOf(status));
        sale = saleRepository.save(sale);
        
        log.info("Sale {} status updated to {}", sale.getInvoiceNumber(), status);
        
        return convertToDTO(sale);
    }
    
    @Transactional
    public void cancelSale(Long id) {
        SaleEntity sale = saleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Venta no encontrada"));
        
        if (sale.getStatus() == SaleEntity.SaleStatus.CANCELLED) {
            throw new BadRequestException("La venta ya está cancelada");
        }
        
        // Return stock
        for (SaleItemEntity item : sale.getItems()) {
            ProductEntity product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            if (product.getStatus() == ProductEntity.ProductStatus.SOLD_OUT && product.getStock() > 0) {
                product.setStatus(ProductEntity.ProductStatus.AVAILABLE);
            }
            productRepository.save(product);
            
            // Record stock movement
            StockMovementEntity movement = new StockMovementEntity();
            movement.setProduct(product);
            movement.setType(StockMovementEntity.MovementType.IN);
            movement.setQuantity(item.getQuantity());
            movement.setStockBefore(product.getStock() - item.getQuantity());
            movement.setStockAfter(product.getStock());
            movement.setReason("Cancelación de venta");
            movement.setUser(sale.getUser());
            movement.setReferenceType("SALE_CANCEL");
            movement.setReferenceId(sale.getId());
            stockMovementRepository.save(movement);
        }
        
        sale.setStatus(SaleEntity.SaleStatus.CANCELLED);
        saleRepository.save(sale);
        
        log.info("Sale {} cancelled", sale.getInvoiceNumber());
    }
    
    public SaleDTO getSaleByInvoiceNumber(String invoiceNumber, Authentication authentication) {
        SaleEntity sale = saleRepository.findByInvoiceNumber(invoiceNumber)
            .orElseThrow(() -> new NotFoundException("Venta no encontrada"));
        
        // Check permission
        UserEntity currentUser = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        
        if (currentUser.getRole() != UserEntity.UserRole.ADMIN && 
            !sale.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("No tienes permiso para ver esta venta");
        }
        
        return convertToDTO(sale);
    }
    
    private String generateInvoiceNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MMdd-HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        return "FAC-" + timestamp;
    }
    
    private SaleDTO convertToDTO(SaleEntity entity) {
        List<SaleDTO.SaleItemDTO> items = entity.getItems().stream()
            .map(item -> SaleDTO.SaleItemDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productCode(item.getProduct().getCode())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .subtotalDiscount(item.getSubtotalDiscount())
                .build())
            .collect(Collectors.toList());
        
        return SaleDTO.builder()
            .id(entity.getId())
            .invoiceNumber(entity.getInvoiceNumber())
            .userId(entity.getUser().getId())
            .userName(entity.getUser().getFullName())
            .userEmail(entity.getUser().getEmail())
            .subtotal(entity.getSubtotal())
            .discount(entity.getDiscount())
            .iva(entity.getIva())
            .total(entity.getTotal())
            .paymentMethod(entity.getPaymentMethod().name())
            .orderType(entity.getOrderType().name())
            .status(entity.getStatus().name())
            .notes(entity.getNotes())
            .paymentReference(entity.getPaymentReference())
            .items(items)
            .created(entity.getCreated())
            .updated(entity.getUpdated())
            .build();
    }
}
