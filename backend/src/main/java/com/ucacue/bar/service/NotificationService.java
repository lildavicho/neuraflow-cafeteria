package com.ucacue.bar.service;

import com.ucacue.bar.entity.NotificationLogEntity;
import com.ucacue.bar.entity.NotificationLogEntity.DeliveryMethod;
import com.ucacue.bar.entity.NotificationLogEntity.DeliveryStatus;
import com.ucacue.bar.entity.NotificationLogEntity.NotificationType;
import com.ucacue.bar.entity.OrderEntity;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final PushService pushService;
    private final EmailService emailService;
    private final NotificationLogRepository notificationLogRepository;

    @Transactional
    public void notifyOrderReady(OrderEntity order, Integer estimatedMinutes) {
        UserEntity user = order.getUser();
        String title = "¡Tu pedido #" + order.getId() + " está listo!";
        String body = "Recógelo en: Mostrador Principal" +
            (estimatedMinutes != null ? ". ETA: " + estimatedMinutes + " min" : "");

        Map<String, String> data = new HashMap<>();
        data.put("orderId", String.valueOf(order.getId()));
        data.put("status", OrderEntity.OrderStatus.READY.name());
        if (estimatedMinutes != null) {
            data.put("estimatedMinutes", String.valueOf(estimatedMinutes));
        }
        data.put("pickupLocation", "Mostrador Principal");
        data.put("timestamp", LocalDateTime.now().toString());

        try {
            pushService.sendNotification(user.getId(), title, body, data);
            logDelivery(order, user, NotificationType.ORDER_READY, DeliveryMethod.FCM, DeliveryStatus.SENT, null);
        } catch (Exception ex) {
            logDelivery(order, user, NotificationType.ORDER_READY, DeliveryMethod.FCM, DeliveryStatus.FAILED, ex.getMessage());
        }

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                emailService.send(user.getEmail(), title, body);
                logDelivery(order, user, NotificationType.ORDER_READY, DeliveryMethod.EMAIL, DeliveryStatus.SENT, null);
            } catch (Exception ex) {
                logDelivery(order, user, NotificationType.ORDER_READY, DeliveryMethod.EMAIL, DeliveryStatus.FAILED, ex.getMessage());
            }
        }

        // Log websocket as sent; delivery is handled by STOMP broker.
        logDelivery(order, user, NotificationType.ORDER_READY, DeliveryMethod.WEBSOCKET, DeliveryStatus.SENT, null);
    }

    @Transactional
    public void notifyEtaUpdate(OrderEntity order, Integer estimatedMinutes) {
        UserEntity user = order.getUser();
        logDelivery(order, user, NotificationType.ETA_UPDATE, DeliveryMethod.WEBSOCKET, DeliveryStatus.SENT, null);
    }

    @Transactional
    public void notifyPickupCall(OrderEntity order, String counter) {
        UserEntity user = order.getUser();
        logDelivery(order, user, NotificationType.PICKUP_CALL, DeliveryMethod.WEBSOCKET, DeliveryStatus.SENT, null);
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            String title = "Llamado a mostrador";
            String body = "Pedido #" + order.getId() + " listo en mostrador " + counter;
            try {
                emailService.send(user.getEmail(), title, body);
                logDelivery(order, user, NotificationType.PICKUP_CALL, DeliveryMethod.EMAIL, DeliveryStatus.SENT, null);
            } catch (Exception ex) {
                logDelivery(order, user, NotificationType.PICKUP_CALL, DeliveryMethod.EMAIL, DeliveryStatus.FAILED, ex.getMessage());
            }
        }
    }

    private void logDelivery(OrderEntity order, UserEntity user, NotificationType type,
                             DeliveryMethod method, DeliveryStatus status, String error) {
        NotificationLogEntity logEntry = new NotificationLogEntity();
        logEntry.setOrder(order);
        logEntry.setUser(user);
        logEntry.setNotificationType(type);
        logEntry.setDeliveryMethod(method);
        logEntry.setStatus(status);
        logEntry.setDeliveredAt(status == DeliveryStatus.DELIVERED ? LocalDateTime.now() : null);
        logEntry.setErrorMessage(error);
        notificationLogRepository.save(logEntry);
    }
}
