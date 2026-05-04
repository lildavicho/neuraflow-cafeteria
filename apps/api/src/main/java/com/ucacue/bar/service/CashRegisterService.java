package com.ucacue.bar.service;

import com.ucacue.bar.entity.CashRegisterEntity;
import com.ucacue.bar.entity.CashRegisterEntity.RegisterStatus;
import com.ucacue.bar.entity.OrderEntity;
import com.ucacue.bar.entity.OrderEntity.PaymentMethod;
import com.ucacue.bar.entity.OrderEntity.TransactionStatus;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.repository.CashRegisterRepository;
import com.ucacue.bar.repository.OrderRepository;
import com.ucacue.bar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CashRegisterService {

    private final CashRegisterRepository cashRegisterRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final TenantContextResolver tenantContextResolver;
    private final AuditService auditService;

    @Transactional
    @CacheEvict(cacheNames = "cash-register-active", allEntries = true)
    public CashRegisterEntity openRegister(String userEmail, BigDecimal openingAmount) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        UserEntity user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BadRequestException("Usuario desactivado");
        }
        if (user.getTenant() == null || user.getTenant().getId() == null || !user.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Usuario no encontrado");
        }

        cashRegisterRepository.findTopByTenantIdAndStatusOrderByOpenedAtDesc(tenantId, RegisterStatus.OPEN)
                .ifPresent(r -> {
                    throw new BadRequestException("Ya existe una caja abierta. Ciérrala antes de abrir una nueva.");
                });

        if (openingAmount == null || openingAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("El monto de apertura no puede ser negativo");
        }

        CashRegisterEntity register = new CashRegisterEntity();
        register.setTenantId(tenantId);
        register.setUser(user);
        register.setOpeningAmount(openingAmount);
        register.setStatus(RegisterStatus.OPEN);

        CashRegisterEntity saved = cashRegisterRepository.save(register);
        log.info("Cash register {} opened by {} with opening amount {}", saved.getId(), userEmail, openingAmount);
        auditService.logAction(userEmail, "CASH_OPEN", "CashRegister", saved.getId(),
                "opening=" + openingAmount);
        return saved;
    }

    @Transactional
    @CacheEvict(cacheNames = "cash-register-active", allEntries = true)
    public CashRegisterEntity closeRegister(Long registerId, BigDecimal actualCash, String notes) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        CashRegisterEntity register = cashRegisterRepository.findByTenantIdAndId(tenantId, registerId)
                .orElseThrow(() -> new NotFoundException("Caja no encontrada"));
        ensureTenantOwnership(register, tenantId);

        if (register.getStatus() == RegisterStatus.CLOSED) {
            throw new BadRequestException("Esta caja ya fue cerrada");
        }

        LocalDateTime openedAt = register.getOpenedAt();
        LocalDateTime now = LocalDateTime.now();
        if (actualCash != null && actualCash.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("El efectivo real no puede ser negativo");
        }

        List<OrderEntity> paidOrders = orderRepository.findByTenantIdAndTransactionStatusAndPaidAtBetween(
                tenantId, TransactionStatus.PAID, openedAt, now);

        BigDecimal cashSales = BigDecimal.ZERO;
        BigDecimal cardSales = BigDecimal.ZERO;
        BigDecimal transferSales = BigDecimal.ZERO;
        int totalOrders = paidOrders.size();

        for (OrderEntity order : paidOrders) {
            BigDecimal orderTotal = order.getEffectiveTotal();
            if (order.getPaymentMethod() == PaymentMethod.CASH) {
                cashSales = cashSales.add(orderTotal);
            } else if (order.getPaymentMethod() == PaymentMethod.CARD) {
                cardSales = cardSales.add(orderTotal);
            } else if (order.getPaymentMethod() == PaymentMethod.TRANSFER) {
                transferSales = transferSales.add(orderTotal);
            }
        }

        BigDecimal totalSales = cashSales.add(cardSales).add(transferSales);
        BigDecimal expectedCash = register.getOpeningAmount().add(cashSales);

        register.setCashSalesTotal(cashSales);
        register.setCardSalesTotal(cardSales);
        register.setTransferSalesTotal(transferSales);
        register.setTotalSalesAmount(totalSales);
        register.setTotalOrders(totalOrders);
        register.setExpectedCash(expectedCash);
        register.setActualCash(actualCash != null ? actualCash : BigDecimal.ZERO);
        register.setDifference(
                (actualCash != null ? actualCash : BigDecimal.ZERO).subtract(expectedCash));
        register.setClosingAmount(actualCash);
        register.setNotes(notes);
        register.setStatus(RegisterStatus.CLOSED);
        register.setClosedAt(now);

        CashRegisterEntity saved = cashRegisterRepository.save(register);
        log.info("Cash register {} closed. Expected: {}, Actual: {}, Diff: {}",
                saved.getId(), expectedCash, actualCash, saved.getDifference());
        auditService.logAction(
                saved.getUser() != null ? saved.getUser().getEmail() : null,
                "CASH_CLOSE", "CashRegister", saved.getId(),
                "expected=" + expectedCash + " actual=" + actualCash + " diff=" + saved.getDifference());
        return saved;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "cash-register-active", key = "#root.target.currentTenantIdForCache()")
    public CashRegisterEntity getActive() {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return cashRegisterRepository.findTopByTenantIdAndStatusOrderByOpenedAtDesc(tenantId, RegisterStatus.OPEN)
                .orElse(null);
    }

    public Long currentTenantIdForCache() {
        return tenantContextResolver.resolveCurrent().getId();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReport(Long registerId) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        CashRegisterEntity register = cashRegisterRepository.findByTenantIdAndId(tenantId, registerId)
                .orElseThrow(() -> new NotFoundException("Caja no encontrada"));
        ensureTenantOwnership(register, tenantId);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", register.getId());
        report.put("status", register.getStatus());
        report.put("userName", register.getUser().getName());
        report.put("openedAt", register.getOpenedAt());
        report.put("closedAt", register.getClosedAt());
        report.put("openingAmount", register.getOpeningAmount());
        report.put("closingAmount", register.getClosingAmount());
        report.put("cashSalesTotal", register.getCashSalesTotal());
        report.put("cardSalesTotal", register.getCardSalesTotal());
        report.put("transferSalesTotal", register.getTransferSalesTotal());
        report.put("totalSalesAmount", register.getTotalSalesAmount());
        report.put("totalOrders", register.getTotalOrders());
        report.put("expectedCash", register.getExpectedCash());
        report.put("actualCash", register.getActualCash());
        report.put("difference", register.getDifference());
        report.put("notes", register.getNotes());
        return report;
    }

    private void ensureTenantOwnership(CashRegisterEntity register, Long tenantId) {
        if (register.getTenantId() == null || !register.getTenantId().equals(tenantId)) {
            throw new NotFoundException("Caja no encontrada");
        }
    }
}
