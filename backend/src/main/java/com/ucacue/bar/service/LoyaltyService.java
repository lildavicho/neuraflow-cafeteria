package com.ucacue.bar.service;

import com.ucacue.bar.dto.loyalty.LoyaltyBalanceResponse;
import com.ucacue.bar.dto.loyalty.LoyaltyLedgerEntryResponse;
import com.ucacue.bar.entity.LoyaltyBalanceEntity;
import com.ucacue.bar.entity.LoyaltyLedgerEntity;
import com.ucacue.bar.entity.OrderEntity;
import com.ucacue.bar.entity.SettingEntity;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.repository.LoyaltyBalanceRepository;
import com.ucacue.bar.repository.LoyaltyLedgerRepository;
import com.ucacue.bar.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyService {

    private static final String SETTING_POINTS_PER_DOLLAR = "points_per_dollar";

    private final LoyaltyBalanceRepository balanceRepository;
    private final LoyaltyLedgerRepository ledgerRepository;
    private final SettingRepository settingRepository;
    private final RealtimeGateway realtimeGateway;
    private final com.ucacue.bar.repository.UserRepository userRepository;
    @Value("${app.loyalty.points-per-dollar:1}")
    private BigDecimal defaultPointsFactor;

    @Transactional
    public int awardForOrder(OrderEntity order) {
        if (order.getUser() == null) {
            return 0;
        }
        int points = calculatePoints(order.getTotal());
        if (points <= 0) {
            return 0;
        }
        LoyaltyBalanceEntity balance = balanceRepository.findByUserId(order.getUser().getId())
            .orElseGet(() -> initBalance(order.getUser()));
        balance.setPoints(balance.getPoints() + points);
        balanceRepository.save(balance);

        LoyaltyLedgerEntity ledger = new LoyaltyLedgerEntity();
        ledger.setUser(order.getUser());
        ledger.setDelta(points);
        ledger.setReason("ORDER:" + order.getId());
        ledgerRepository.save(ledger);

        emitRealtime(order.getUser().getId(), balance.getPoints());
        log.info("Awarded {} loyalty points to user {} for order {}", points, order.getUser().getEmail(), order.getId());
        return points;
    }

    @Transactional(readOnly = true)
    public LoyaltyBalanceResponse getBalance(Long userId) {
        LoyaltyBalanceEntity balance = balanceRepository.findByUserId(userId)
            .orElse(null);
        int points = balance != null ? balance.getPoints() : 0;
        return LoyaltyBalanceResponse.builder()
            .userId(userId)
            .points(points)
            .tier(resolveTier(points))
            .updatedAt(balance != null ? balance.getUpdatedAt() : null)
            .build();
    }

    @Transactional(readOnly = true)
    public LoyaltyBalanceResponse getBalanceByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        return getBalance(user.getId());
    }

    @Transactional(readOnly = true)
    public List<LoyaltyLedgerEntryResponse> getLedger(Long userId) {
        return ledgerRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(entry -> LoyaltyLedgerEntryResponse.builder()
                .id(entry.getId())
                .delta(entry.getDelta())
                .reason(entry.getReason())
                .createdAt(entry.getCreatedAt())
                .build())
            .toList();
    }

    private int calculatePoints(BigDecimal total) {
        BigDecimal factor = resolvePointsFactor();
        return total.multiply(factor)
            .setScale(0, RoundingMode.DOWN)
            .intValue();
    }

    private BigDecimal resolvePointsFactor() {
        return settingRepository.findByKey(SETTING_POINTS_PER_DOLLAR)
            .map(SettingEntity::getValue)
            .map(value -> {
                try {
                    return new BigDecimal(value);
                } catch (Exception ex) {
                    log.warn("Invalid points_per_dollar setting {}, using default 1", value);
                    return defaultPointsFactor != null ? defaultPointsFactor : BigDecimal.ONE;
                }
            })
            .orElse(defaultPointsFactor != null ? defaultPointsFactor : BigDecimal.ONE);
    }

    private String resolveTier(int points) {
        if (points >= 500) {
            return "GOLD";
        }
        if (points >= 200) {
            return "SILVER";
        }
        return "BRONZE";
    }

    private LoyaltyBalanceEntity initBalance(UserEntity user) {
        LoyaltyBalanceEntity entity = new LoyaltyBalanceEntity();
        entity.setUser(user);
        entity.setPoints(0);
        return balanceRepository.save(entity);
    }

    private void emitRealtime(Long userId, int points) {
        realtimeGateway.loyalty(java.util.Map.of(
            "userId", userId,
            "points", points,
            "tier", resolveTier(points)
        ));
    }
}
