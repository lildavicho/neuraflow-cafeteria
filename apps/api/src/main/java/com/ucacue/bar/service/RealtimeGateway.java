package com.ucacue.bar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeGateway {

    public static final String TOPIC_DASHBOARD = "/topic/dashboard";
    public static final String TOPIC_SALES = "/topic/sales";
    public static final String TOPIC_LOYALTY = "/topic/loyalty";
    public static final String TOPIC_SETTINGS = "/topic/settings";
    public static final String TOPIC_PEOPLE_COUNTS = "/topic/people_counts";
    public static final String TOPIC_PRODUCTS = "/topic/products";
    public static final String TOPIC_INVENTORY = "/topic/inventory";
    public static final String TOPIC_NOTIFICATIONS = "/topic/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public void send(String topic, Object payload) {
        try {
            messagingTemplate.convertAndSend(topic, payload);
        } catch (Exception ex) {
            log.debug("WS dispatch to {} failed: {}", topic, ex.getMessage());
        }
    }

    public void dashboard(Object payload) {
        send(TOPIC_DASHBOARD, payload);
    }

    public void sales(Object payload) {
        send(TOPIC_SALES, payload);
    }

    public void loyalty(Object payload) {
        send(TOPIC_LOYALTY, payload);
    }

    public void settings(Object payload) {
        send(TOPIC_SETTINGS, payload);
    }

    public void people(Object payload) {
        send(TOPIC_PEOPLE_COUNTS, payload);
    }

    public void products(Object payload) {
        send(TOPIC_PRODUCTS, payload);
    }

    public void inventory(Object payload) {
        send(TOPIC_INVENTORY, payload);
    }

    public void notifyChannel(Object payload) {
        send(TOPIC_NOTIFICATIONS, payload);
    }

    public void orders(String subTopic, Object payload) {
        send("/topic/orders/" + subTopic, payload);
    }
}
