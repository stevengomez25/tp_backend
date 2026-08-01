package com.tempmailpro.application.services;

import com.tempmailpro.domain.models.*;
import com.tempmailpro.infrastructure.adapters.JpaOrderRepository;
import com.tempmailpro.infrastructure.adapters.JpaSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    // Inyecciones de dependencias correctas
    private final JpaOrderRepository orderRepository;
    private final JpaSubscriptionRepository subscriptionRepository;

    @Value("${wompi.webhook.events-secret}")
    private String webhookSecret;

    @Value("${wompi.integrity-secret}")
    private String integritySecret;


    /**
     * 1. Crea la orden de pago inicial en nuestra DB
     */
    @Transactional
    public Order createOrder(User user, PlanType targetPlan, Long amountInCents) {
        String uniqueReference = "TMP-OR-" + UUID.randomUUID().toString().substring(0, 18);

        Order order = Order.builder()
                .reference(uniqueReference)
                .organization(user.getOrganization())
                .user(user)
                .amountInCents(amountInCents)
                .currency("COP")
                .targetPlan(targetPlan)
                .status("PENDING")
                .build();

        return orderRepository.save(order);
    }

    /**
     * Genera la firma de integridad requerida por el Widget de Wompi (Arreglado sin appends raros)
     */
    public String generateIntegritySignature(String reference, Long amountInCents, String currency) {
        try {
            String concatenated = reference + amountInCents + currency + integritySecret;
            return bytesToHex(calculateSha256(concatenated));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al generar firma de integridad", e);
        }
    }

    /**
     * 2. Procesa la respuesta del Webhook de Wompi de forma segura
     */
    @Transactional
    public void processWebhook(String reference, String wompiStatus, String receivedHash, String timestamp) {

        Order order = orderRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Orden de pago no encontrada para la referencia: " + reference));

        if (!"PENDING".equals(order.getStatus())) {
            return;
        }

        // VALIDACIÓN DE SEGURIDAD (Anti-Fraude)
        if (!isValidWompiSignature(reference, wompiStatus, order.getAmountInCents().toString(), timestamp, receivedHash)) {
            order.setStatus("ERROR_SIGNATURE_MISMATCH");
            orderRepository.save(order);
            throw new SecurityException("Firma del webhook de Wompi inválida. Intento de fraude detectado.");
        }

        if ("APPROVED".equals(wompiStatus)) {
            order.setStatus("APPROVED");
            activateSubscription(order.getOrganization(), order.getTargetPlan(), order.getBillingCycle());
        } else {
            order.setStatus(wompiStatus);
        }

        orderRepository.save(order);
    }

    private void activateSubscription(Organization organization, PlanType planType, BillingCycle billingCycle) {
        Subscription subscription = subscriptionRepository.findByOrganizationId(organization.getId())
                .orElse(Subscription.builder()
                        .organization(organization)
                        .build());

        LocalDateTime startFrom = (subscription.getEndsAt() != null && subscription.getEndsAt().isAfter(LocalDateTime.now()))
                ? subscription.getEndsAt()
                : LocalDateTime.now();

        // AQUÍ USA EL PARÁMETRO ENTRANTE DIRECTAMENTE:
        int daysToExtend = (billingCycle == BillingCycle.YEARLY) ? 365 : 30;

        subscription.setPlanType(planType);
        subscription.setBillingCycle(billingCycle); // Lo guardamos también en la suscripción
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setEndsAt(startFrom.plusDays(daysToExtend));

        subscriptionRepository.save(subscription);
    }

    /**
     * Validación de firma del Webhook arreglada usando funciones auxiliares limpias
     */
    private boolean isValidWompiSignature(String reference, String status, String amountCents, String timestamp, String receivedHash) {
        try {
            String concatenatedProperties = reference + status + amountCents + timestamp + webhookSecret;
            String calculatedHash = bytesToHex(calculateSha256(concatenatedProperties));
            return calculatedHash.equals(receivedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculando el Hash SHA-256 de seguridad", e);
        }
    }

    // ==========================================
    // MÉTODOS AUXILIARES DE ENCRIPTACIÓN (MÁS LIMPIOS Y SEGUROS)
    // ==========================================

    private byte[] calculateSha256(String text) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(text.getBytes(StandardCharsets.UTF_8));
    }

    private String bytesToHex(byte[] hashBytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            // Convierte de forma segura cada byte a hexadecimal de 2 caracteres sin romper la sintaxis
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    private Long getBaseMonthlyPriceInCents(PlanType planType) {
        return switch (planType) {
            case FREE -> 0L;
            case PREMIUM -> 1900000L;       // Ej: $19.000 COP
            case PREMIUM_PLUS -> 3900000L;  // Ej: $39.000 COP
            case STARTER_TEAM -> 9900000L;  // Ej: $99.000 COP (Hasta 10 usuarios)
            case GROWTH_TEAM -> 24900000L;  // Ej: $249.000 COP (Hasta 25 usuarios)
            case SCALE_UP -> 59900000L;     // Ej: $599.000 COP (Hasta 100 usuarios)
            case ENTERPRISE -> 199900000L;  // Ej: $1'999.000 COP
        };
    }

    /**
     * Calcula el monto total aplicando el 20% de descuento si es pago anual
     */
    public Long calculateTotalPrice(PlanType planType, BillingCycle cycle) {
        Long basePrice = getBaseMonthlyPriceInCents(planType);

        if (cycle == BillingCycle.YEARLY) {
            // 12 meses con el 20% de descuento total -> Monto * 12 * 0.8
            return Double.valueOf(basePrice * 12 * 0.8).longValue();
        }

        return basePrice;
    }
}