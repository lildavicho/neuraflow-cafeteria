package com.ucacue.bar.service;

import com.ucacue.bar.entity.OrderEntity;
import com.ucacue.bar.entity.SaleEntity;
import com.ucacue.bar.entity.SaleEntity.SaleStatus;
import com.ucacue.bar.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {

    private final SaleRepository saleRepository;
    private final RealtimeGateway realtimeGateway;

    @Transactional
    public SaleEntity registerSale(OrderEntity order) {
        return saleRepository.findByOrderId(order.getId())
                .map(existing -> refreshPaidSale(existing, order))
                .orElseGet(() -> persistSale(order));
    }

    @Transactional
    public void markSaleRefunded(OrderEntity order) {
        saleRepository.findByOrderId(order.getId()).ifPresent(sale -> {
            sale.setSaleStatus(SaleStatus.REFUNDED);
            sale.setRefundedAt(order.getRefundedAt() != null ? order.getRefundedAt() : LocalDateTime.now());
            saleRepository.save(sale);
            realtimeGateway.sales(java.util.Map.of(
                    "type", "sale-refunded",
                    "orderId", order.getId(),
                    "total", order.getTotal()));
        });
    }

    private SaleEntity refreshPaidSale(SaleEntity sale, OrderEntity order) {
        sale.setTotal(order.getTotal());
        sale.setSaleStatus(SaleStatus.PAID);
        sale.setRefundedAt(null);
        sale.setPaidAt(order.getPaidAt() != null ? order.getPaidAt() : LocalDateTime.now());
        return saleRepository.save(sale);
    }

    private SaleEntity persistSale(OrderEntity order) {
        SaleEntity sale = new SaleEntity();
        if (order.getTenantId() != null) {
            sale.setTenantId(order.getTenantId());
        }
        sale.setOrder(order);
        sale.setTotal(order.getTotal());
        sale.setSaleStatus(SaleStatus.PAID);
        sale.setRefundedAt(null);
        sale.setPaidAt(order.getPaidAt() != null ? order.getPaidAt() : LocalDateTime.now());

        SaleEntity saved = saleRepository.save(sale);
        realtimeGateway.sales(java.util.Map.of(
                "type", "sale-recorded",
                "orderId", order.getId(),
                "total", order.getTotal()));
        return saved;
    }
}
