package com.ucacue.bar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void send(String to, String subject, String body) {
        int size = body != null ? body.length() : 0;
        log.debug("Email send requested to={} subject={} bodySize={}", to, subject, size);
    }
}
