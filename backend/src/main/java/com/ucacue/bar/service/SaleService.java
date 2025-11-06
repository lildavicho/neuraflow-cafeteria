package com.ucacue.bar.service;

import com.ucacue.bar.entity.OrderEntity;
import com.ucacue.bar.entity.OrderItemEntity;
import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.entity.SaleEntity;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.repository.ProductRepository;
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
    private final ProductRepository productRepository;
    private final RealtimeGateway realtimeGateway;

    @Transactional
    public SaleEntity registerSale(OrderEntity order) {
        return saleRepository.findByOrderId(order.getId()).orElseGet(() -> persistSale(order));
    }

    private SaleEntity persistSale(OrderEntity order) {
        adjustInventory(order);

        SaleEntity sale = new SaleEntity();
        sale.setOrder(order);
        sale.setTotal(order.getTotal());
        sale.setPaidAt(LocalDateTime.now());

        SaleEntity saved = saleRepository.save(sale);
        realtimeGateway.sales(java.util.Map.of(
            "type", "sale-recorded",
            "orderId", order.getId(),
            "total", order.getTotal()
        ));
        return saved;
    }

    private void adjustInventory(OrderEntity order) {
        for (OrderItemEntity item : order.getItems()) {
            ProductEntity product = productRepository.findById(item.getProduct().getId())
                .orElseThrow(() -> new BadRequestException("Producto no encontrado: " + item.getProduct().getId()));

            // Stock ya fue decrementado al crear la orden; evitar doble decremento aquí.
            if (product.getStock() <= 0) {
                product.setStatus(ProductEntity.ProductStatus.SOLD_OUT);
                productRepository.save(product);
            }

            realtimeGateway.inventory(java.util.Map.of(
                "type", "stock",
                "productId", product.getId(),
                "stock", product.getStock()
            ));
        }
    }
}
