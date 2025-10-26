package com.ucacue.bar.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${spring.application.name:UCACUE Bar}")
    private String appName;
    
    @Async
    public void sendWelcomeEmail(String to, String userName) {
        try {
            String subject = "Bienvenido a " + appName;
            String htmlContent = getWelcomeEmailTemplate(userName);
            sendHtmlEmail(to, subject, htmlContent);
        } catch (Exception e) {
            log.error("Error sending welcome email to {}: {}", to, e.getMessage());
        }
    }
    
    @Async
    public void sendTwoFactorCode(String to, String code) {
        try {
            String subject = "Código de verificación - " + appName;
            String htmlContent = getTwoFactorEmailTemplate(code);
            sendHtmlEmail(to, subject, htmlContent);
        } catch (Exception e) {
            log.error("Error sending 2FA code to {}: {}", to, e.getMessage());
        }
    }
    
    @Async
    public void sendPasswordResetEmail(String to, String resetToken) {
        try {
            String subject = "Restablecer contraseña - " + appName;
            String htmlContent = getPasswordResetTemplate(resetToken);
            sendHtmlEmail(to, subject, htmlContent);
        } catch (Exception e) {
            log.error("Error sending password reset email to {}: {}", to, e.getMessage());
        }
    }
    
    @Async
    public void sendLowStockAlert(String to, String productName, int currentStock) {
        try {
            String subject = "Alerta de Stock Bajo - " + productName;
            String htmlContent = getLowStockAlertTemplate(productName, currentStock);
            sendHtmlEmail(to, subject, htmlContent);
        } catch (Exception e) {
            log.error("Error sending low stock alert: {}", e.getMessage());
        }
    }
    
    @Async
    public void sendOrderConfirmation(String to, String orderNumber, String total) {
        try {
            String subject = "Confirmación de Orden #" + orderNumber;
            String htmlContent = getOrderConfirmationTemplate(orderNumber, total);
            sendHtmlEmail(to, subject, htmlContent);
        } catch (Exception e) {
            log.error("Error sending order confirmation to {}: {}", to, e.getMessage());
        }
    }
    
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        
        mailSender.send(message);
        log.info("Email sent successfully to: {}", to);
    }
    
    private String getWelcomeEmailTemplate(String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #b30000; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background: #f7f7f7; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    .btn { display: inline-block; padding: 10px 20px; background: #b30000; color: white; text-decoration: none; border-radius: 5px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>¡Bienvenido a UCACUE Bar!</h1>
                    </div>
                    <div class="content">
                        <h2>Hola %s,</h2>
                        <p>¡Gracias por registrarte en nuestro sistema!</p>
                        <p>Ya puedes acceder a nuestra cafetería y realizar tus compras de manera rápida y segura.</p>
                        <p>Recuerda que puedes:</p>
                        <ul>
                            <li>Ver el menú disponible</li>
                            <li>Realizar pedidos en línea</li>
                            <li>Ver tu historial de compras</li>
                            <li>Recibir notificaciones de promociones</li>
                        </ul>
                        <p style="text-align: center; margin-top: 30px;">
                            <a href="#" class="btn">Ir a la Cafetería</a>
                        </p>
                    </div>
                    <div class="footer">
                        <p>© 2025 UCACUE Bar. Todos los derechos reservados.</p>
                        <p>Universidad Católica de Cuenca</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName);
    }
    
    private String getTwoFactorEmailTemplate(String code) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #b30000; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background: #f7f7f7; }
                    .code-box { background: white; border: 2px solid #b30000; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; letter-spacing: 5px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Código de Verificación</h1>
                    </div>
                    <div class="content">
                        <p>Has solicitado un código de verificación para acceder a tu cuenta.</p>
                        <p>Tu código de verificación es:</p>
                        <div class="code-box">%s</div>
                        <p><strong>Este código expirará en 5 minutos.</strong></p>
                        <p>Si no has solicitado este código, por favor ignora este mensaje.</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 UCACUE Bar. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(code);
    }
    
    private String getPasswordResetTemplate(String resetToken) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #b30000; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background: #f7f7f7; }
                    .btn { display: inline-block; padding: 12px 30px; background: #b30000; color: white; text-decoration: none; border-radius: 5px; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Restablecer Contraseña</h1>
                    </div>
                    <div class="content">
                        <p>Has solicitado restablecer tu contraseña.</p>
                        <p>Haz clic en el siguiente enlace para crear una nueva contraseña:</p>
                        <p style="text-align: center; margin: 30px 0;">
                            <a href="http://localhost:3001/reset-password?token=%s" class="btn">Restablecer Contraseña</a>
                        </p>
                        <p><strong>Este enlace expirará en 1 hora.</strong></p>
                        <p>Si no has solicitado este cambio, puedes ignorar este mensaje.</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 UCACUE Bar. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(resetToken);
    }
    
    private String getLowStockAlertTemplate(String productName, int currentStock) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #ff9800; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background: #f7f7f7; }
                    .alert-box { background: #fff3cd; border: 1px solid #ffc107; padding: 15px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>⚠ Alerta de Stock Bajo</h1>
                    </div>
                    <div class="content">
                        <div class="alert-box">
                            <h3>Producto: %s</h3>
                            <p><strong>Stock Actual: %d unidades</strong></p>
                        </div>
                        <p>El stock de este producto está por debajo del mínimo recomendado.</p>
                        <p>Se recomienda realizar un pedido de reabastecimiento lo antes posible.</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 UCACUE Bar. Sistema de Gestión de Inventario.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(productName, currentStock);
    }
    
    private String getOrderConfirmationTemplate(String orderNumber, String total) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #4caf50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background: #f7f7f7; }
                    .order-info { background: white; padding: 20px; margin: 20px 0; }
                    .total { font-size: 24px; color: #b30000; font-weight: bold; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✓ Orden Confirmada</h1>
                    </div>
                    <div class="content">
                        <h2>¡Gracias por tu compra!</h2>
                        <div class="order-info">
                            <p><strong>Número de Orden:</strong> #%s</p>
                            <p class="total">Total: $%s</p>
                        </div>
                        <p>Tu pedido ha sido confirmado y está siendo preparado.</p>
                        <p>Recibirás una notificación cuando esté listo para recoger.</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 UCACUE Bar. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(orderNumber, total);
    }
}
