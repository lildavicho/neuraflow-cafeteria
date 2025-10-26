package com.ucacue.bar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucacue.bar.dto.SaleDTO;
import com.ucacue.bar.entity.IdempotentRequestEntity;
import com.ucacue.bar.repository.IdempotentRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {
    
    private final IdempotentRequestRepository repository;
    private final ObjectMapper objectMapper;
    
    private static final int CACHE_DURATION_HOURS = 24;
    
    @Transactional
    public ResponseEntity<SaleDTO> getCachedResponse(String idempotencyKey) {
        // Clean expired entries
        cleanExpiredEntries();
        
        Optional<IdempotentRequestEntity> cached = repository.findByKey(idempotencyKey);
        
        if (cached.isPresent()) {
            try {
                IdempotentRequestEntity entity = cached.get();
                SaleDTO sale = objectMapper.readValue(entity.getResponse(), SaleDTO.class);
                log.debug("Returning cached response for idempotency key: {}", idempotencyKey);
                return ResponseEntity.status(entity.getStatusCode()).body(sale);
            } catch (Exception e) {
                log.error("Error deserializing cached response", e);
                return null;
            }
        }
        
        return null;
    }
    
    @Transactional
    public void cacheResponse(String idempotencyKey, ResponseEntity<SaleDTO> response) {
        try {
            String responseJson = objectMapper.writeValueAsString(response.getBody());
            
            IdempotentRequestEntity entity = new IdempotentRequestEntity();
            entity.setKey(idempotencyKey);
            entity.setResponse(responseJson);
            entity.setStatusCode(response.getStatusCode().value());
            entity.setExpiresAt(LocalDateTime.now().plusHours(CACHE_DURATION_HOURS));
            
            repository.save(entity);
            log.debug("Cached response for idempotency key: {}", idempotencyKey);
        } catch (Exception e) {
            log.error("Error caching response", e);
        }
    }
    
    @Transactional
    public void cleanExpiredEntries() {
        try {
            int deleted = repository.deleteExpiredEntries(LocalDateTime.now());
            if (deleted > 0) {
                log.debug("Cleaned {} expired idempotency entries", deleted);
            }
        } catch (Exception e) {
            log.error("Error cleaning expired entries", e);
        }
    }
}
