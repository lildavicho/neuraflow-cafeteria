package com.ucacue.bar.service;

import com.ucacue.bar.algo.PreparationTimePredictor;
import com.ucacue.bar.dto.order.OrderCreateRequest;
import com.ucacue.bar.dto.order.OrderResponse;
import com.ucacue.bar.entity.CategoryEntity;
import com.ucacue.bar.entity.OrderEntity;
import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.entity.SaleEntity;
import com.ucacue.bar.entity.StockMovementEntity;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.erp.accounting.application.SalesAccountingService;
import com.ucacue.bar.erp.accounting.application.TaxComputationService;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxComputationType;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxScope;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxRuleEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxRuleRepository;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.repository.TenantRepository;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.repository.CategoryRepository;
import com.ucacue.bar.repository.OrderItemRepository;
import com.ucacue.bar.repository.OrderRepository;
import com.ucacue.bar.repository.ProductRepository;
import com.ucacue.bar.repository.StockMovementRepository;
import com.ucacue.bar.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import({
        OrderService.class,
        TaxComputationService.class,
        TenantContextResolver.class
})
class OrderServiceConsistencyIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private TaxRuleRepository taxRuleRepository;

    @MockitoBean
    private LoyaltyService loyaltyService;

    @MockitoBean
    private SaleService saleService;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private RealtimeGateway realtimeGateway;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private PreparationTimePredictor preparationTimePredictor;

    @MockitoBean
    private MlPredictionService mlPredictionService;

    @MockitoBean
    private SalesAccountingService salesAccountingService;

    @MockitoBean
    private AuditService auditService;

    private UserEntity user;
    private ProductEntity product;

    @BeforeEach
    void setUp() {
        TenantEntity tenant = new TenantEntity();
        tenant.setTenantCode("default");
        tenant.setLegalName("Tenant Default");
        tenant.setTradeName("Tenant Default");
        tenant = tenantRepository.save(tenant);
        Long tenantId = tenant.getId();

        TaxRuleEntity iva = new TaxRuleEntity();
        iva.setTenantId(tenantId);
        iva.setTaxCode("IVA_GENERAL");
        iva.setTaxName("IVA 15%");
        iva.setTaxScope(TaxScope.DOCUMENT);
        iva.setComputationType(TaxComputationType.PERCENTAGE);
        iva.setRate(BigDecimal.ZERO);
        iva.setIncludedInPrice(Boolean.FALSE);
        iva.setActive(Boolean.TRUE);
        taxRuleRepository.save(iva);

        CategoryEntity category = new CategoryEntity();
        category.setTenantId(tenantId);
        category.setName("Bebidas Consistencia");
        category.setActive(Boolean.TRUE);
        category = categoryRepository.save(category);

        user = new UserEntity();
        user.setTenant(tenant);
        user.setName("Cajero Demo");
        user.setEmail("cashier@example.com");
        user.setIdentification("0912345678");
        user.setRole(UserEntity.UserRole.CASHIER);
        user.setActive(Boolean.TRUE);
        user = userRepository.save(user);

        product = new ProductEntity();
        product.setTenantId(tenantId);
        product.setCategory(category);
        product.setCode("CONS-001");
        product.setName("Cola");
        product.setDescription("Bebida");
        product.setUnit("UNIDAD");
        product.setPrice(new BigDecimal("5.00"));
        product.setPurchasePrice(new BigDecimal("2.00"));
        product.setStock(10);
        product.setMinStock(2);
        product.setStatus(ProductEntity.ProductStatus.AVAILABLE);
        product.setPrepared(Boolean.TRUE);
        product = productRepository.save(product);

        when(loyaltyService.awardForOrder(any())).thenReturn(7);
        when(saleService.registerSale(any())).thenReturn(new SaleEntity());
    }

    private Authentication authFor(String email) {
        return new UsernamePasswordAuthenticationToken(email, "n/a", java.util.List.of());
    }

    @Test
    void createOrderShouldReserveInventoryAndOnlyCommitStockOnPayment() {
        OrderResponse created = orderService.createOrder(orderRequest(3), authFor(user.getEmail()));

        ProductEntity reservedProduct = productRepository.findById(product.getId()).orElseThrow();
        Integer reservedUnits = orderItemRepository.sumReservedQuantityByProductId(product.getId());
        List<StockMovementEntity> movementsAfterCreate = stockMovementRepository.findAll();

        assertThat(created.getInventoryStatus()).isEqualTo(OrderEntity.InventoryStatus.RESERVED);
        assertThat(created.getTransactionStatus()).isEqualTo(OrderEntity.TransactionStatus.PENDING_PAYMENT);
        assertThat(created.getTotal()).isEqualByComparingTo("15.00");
        assertThat(reservedProduct.getStock()).isEqualTo(10);
        assertThat(reservedUnits).isEqualTo(3);
        assertThat(movementsAfterCreate).hasSize(1);
        assertThat(movementsAfterCreate.get(0).getType()).isEqualTo(StockMovementEntity.MovementType.RESERVED);
        assertThat(movementsAfterCreate.get(0).getStockBefore()).isEqualTo(10);
        assertThat(movementsAfterCreate.get(0).getStockAfter()).isEqualTo(10);

        OrderResponse paid = orderService.pay(created.getId(), "cash", "PAY-001");

        ProductEntity committedProduct = productRepository.findById(product.getId()).orElseThrow();
        Integer reservedAfterPay = orderItemRepository.sumReservedQuantityByProductId(product.getId());
        List<StockMovementEntity> movementsAfterPay = stockMovementRepository.findAll();

        assertThat(paid.getInventoryStatus()).isEqualTo(OrderEntity.InventoryStatus.COMMITTED);
        assertThat(paid.getTransactionStatus()).isEqualTo(OrderEntity.TransactionStatus.PAID);
        assertThat(committedProduct.getStock()).isEqualTo(7);
        assertThat(reservedAfterPay).isZero();
        assertThat(movementsAfterPay).hasSize(2);
        assertThat(movementsAfterPay)
                .extracting(StockMovementEntity::getType)
                .containsExactlyInAnyOrder(
                        StockMovementEntity.MovementType.RESERVED,
                        StockMovementEntity.MovementType.COMMITTED
                );
        verify(loyaltyService).awardForOrder(any());
        verify(saleService).registerSale(any());
        verify(salesAccountingService).postPaidOrder(any());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentPaymentsShouldOnlyApplySideEffectsOnce() throws Exception {
        OrderResponse created = orderService.createOrder(orderRequest(3), authFor(user.getEmail()));

        CountDownLatch firstInsideSale = new CountDownLatch(1);
        CountDownLatch releaseSale = new CountDownLatch(1);
        AtomicInteger saleCalls = new AtomicInteger();
        doAnswer(invocation -> {
            if (saleCalls.incrementAndGet() == 1) {
                firstInsideSale.countDown();
                assertThat(releaseSale.await(2, TimeUnit.SECONDS)).isTrue();
            }
            return new SaleEntity();
        }).when(saleService).registerSale(any());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<OrderResponse> firstPayment = executor.submit(() -> orderService.pay(created.getId(), "cash", "PAY-A"));
            assertThat(firstInsideSale.await(2, TimeUnit.SECONDS)).isTrue();

            Future<OrderResponse> secondPayment = executor.submit(() -> orderService.pay(created.getId(), "cash", "PAY-B"));
            Thread.sleep(200);
            assertThat(secondPayment.isDone()).isFalse();

            releaseSale.countDown();

            OrderResponse first = firstPayment.get(5, TimeUnit.SECONDS);
            OrderResponse second = secondPayment.get(5, TimeUnit.SECONDS);

            ProductEntity persistedProduct = productRepository.findById(product.getId()).orElseThrow();
            OrderEntity persistedOrder = orderRepository.findById(created.getId()).orElseThrow();

            assertThat(first.getTransactionStatus()).isEqualTo(OrderEntity.TransactionStatus.PAID);
            assertThat(second.getTransactionStatus()).isEqualTo(OrderEntity.TransactionStatus.PAID);
            assertThat(persistedOrder.getTransactionStatus()).isEqualTo(OrderEntity.TransactionStatus.PAID);
            assertThat(persistedOrder.getInventoryStatus()).isEqualTo(OrderEntity.InventoryStatus.COMMITTED);
            assertThat(persistedProduct.getStock()).isEqualTo(7);
            verify(loyaltyService, times(1)).awardForOrder(any());
            verify(saleService, times(1)).registerSale(any());
            verify(salesAccountingService, times(1)).postPaidOrder(any());
        } finally {
            releaseSale.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentPayAndCancelShouldEndInConsistentPaidState() throws Exception {
        OrderResponse created = orderService.createOrder(orderRequest(3), authFor(user.getEmail()));

        CountDownLatch firstInsideSale = new CountDownLatch(1);
        CountDownLatch releaseSale = new CountDownLatch(1);
        doAnswer(invocation -> {
            firstInsideSale.countDown();
            assertThat(releaseSale.await(2, TimeUnit.SECONDS)).isTrue();
            return new SaleEntity();
        }).when(saleService).registerSale(any());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<OrderResponse> payFuture = executor.submit(() -> orderService.pay(created.getId(), "cash", "PAY-C"));
            assertThat(firstInsideSale.await(2, TimeUnit.SECONDS)).isTrue();

            Future<OrderResponse> cancelFuture = executor.submit(() -> orderService.cancel(created.getId(), "cancelar"));
            Thread.sleep(200);
            assertThat(cancelFuture.isDone()).isFalse();

            releaseSale.countDown();

            OrderResponse paid = payFuture.get(5, TimeUnit.SECONDS);
            assertThat(paid.getTransactionStatus()).isEqualTo(OrderEntity.TransactionStatus.PAID);

            assertThatThrownBy(() -> cancelFuture.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(BadRequestException.class)
                    .hasRootCauseMessage("Una orden pagada debe pasar por reembolso, no por cancelacion");

            OrderEntity persistedOrder = orderRepository.findById(created.getId()).orElseThrow();
            ProductEntity persistedProduct = productRepository.findById(product.getId()).orElseThrow();
            assertThat(persistedOrder.getTransactionStatus()).isEqualTo(OrderEntity.TransactionStatus.PAID);
            assertThat(persistedOrder.getStatus()).isEqualTo(OrderEntity.OrderStatus.PENDING);
            assertThat(persistedOrder.getInventoryStatus()).isEqualTo(OrderEntity.InventoryStatus.COMMITTED);
            assertThat(persistedProduct.getStock()).isEqualTo(7);
            verify(loyaltyService, times(1)).awardForOrder(any());
            verify(saleService, times(1)).registerSale(any());
            verify(salesAccountingService, times(1)).postPaidOrder(any());
        } finally {
            releaseSale.countDown();
            executor.shutdownNow();
        }
    }

    private OrderCreateRequest orderRequest(int quantity) {
        return OrderCreateRequest.builder()
                .paymentMethod(OrderCreateRequest.OrderPaymentMethod.CASH)
                .items(List.of(OrderCreateRequest.Item.builder()
                        .productId(product.getId())
                        .quantity(quantity)
                        .unitPrice(product.getPrice())
                        .build()))
                .notes("test")
                .build();
    }
}
