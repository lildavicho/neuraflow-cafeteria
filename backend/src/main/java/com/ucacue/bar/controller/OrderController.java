package com.ucacue.bar.controller;

import com.ucacue.bar.dto.order.OrderCancelRequest;
import com.ucacue.bar.dto.order.OrderCreateRequest;
import com.ucacue.bar.dto.order.OrderDeliverRequest;
import com.ucacue.bar.dto.order.OrderPaymentRequest;
import com.ucacue.bar.dto.order.OrderPriceRequest;
import com.ucacue.bar.dto.order.OrderResponse;
import com.ucacue.bar.entity.OrderEntity;
import com.ucacue.bar.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

import java.util.Optional;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Gestión de pedidos POS")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BUYER')")
    @Operation(summary = "Crear una nueva orden desde el POS")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderCreateRequest request,
                                                     Authentication authentication) {
        OrderResponse response = orderService.createOrder(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar órdenes con paginación")
    public ResponseEntity<Page<OrderResponse>> listOrders(@RequestParam Optional<OrderEntity.OrderStatus> status,
                                                          Pageable pageable) {
        return ResponseEntity.ok(orderService.list(status.orElse(null), pageable));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aceptar una orden pendiente")
    public ResponseEntity<OrderResponse> accept(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.accept(id));
    }

    @PostMapping("/{id}/ready")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Marcar una orden como lista")
    public ResponseEntity<OrderResponse> ready(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.ready(id));
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Entregar una orden y registrar la venta")
    public ResponseEntity<OrderResponse> deliver(@PathVariable Long id,
                                                 @Valid @RequestBody(required = false) OrderDeliverRequest request) {
        String reference = request != null ? request.getPaymentReference() : null;
        return ResponseEntity.ok(orderService.deliver(id, reference));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancelar una orden")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long id,
                                                @Valid @RequestBody(required = false) OrderCancelRequest request) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(orderService.cancel(id, reason));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BUYER')")
    @Operation(summary = "Detalle de una orden")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.get(id));
    }

    @PostMapping("/prices")
    @PreAuthorize("hasAnyRole('ADMIN','BUYER')")
    @Operation(summary = "Calcular totales del carrito sin crear la orden")
    public ResponseEntity<Map<String, Object>> preview(@Valid @RequestBody OrderPriceRequest request) {
        return ResponseEntity.ok(orderService.previewTotals(request.getItems()));
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN','BUYER')")
    @Operation(summary = "Registrar pago de una orden (UI propia)")
    public ResponseEntity<OrderResponse> pay(@PathVariable Long id,
                                             @Valid @RequestBody OrderPaymentRequest request) {
        return ResponseEntity.ok(orderService.pay(id, request.getMethod(), request.getReference()));
    }

    @PostMapping("/{id}/confirm-payment")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Confirmar pago de una orden (transferencia)")
    public ResponseEntity<OrderResponse> confirmPayment(@PathVariable Long id,
                                                        @RequestBody(required = false) OrderDeliverRequest request) {
        String reference = request != null ? request.getPaymentReference() : null;
        return ResponseEntity.ok(orderService.confirmPayment(id, reference));
    }
}
