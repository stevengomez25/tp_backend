package com.tempmailpro.infrastructure.controllers;

import com.tempmailpro.application.services.PaymentService;
import com.tempmailpro.domain.models.Order;
import com.tempmailpro.domain.models.PlanType;
import com.tempmailpro.domain.models.User;
import com.tempmailpro.infrastructure.adapters.JpaOrderRepository;
import com.tempmailpro.infrastructure.adapters.JpaUserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final JpaUserRepository userRepository;
    private final JpaOrderRepository orderRepository;

    /**
     * ENDPOINT 1: Generar orden de pago antes de abrir el Widget de Wompi
     * ACCESO: Autenticado vía JWT
     */
    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(@RequestBody CheckoutRequest request) {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 1. Guardamos la orden pendiente en nuestra DB
        Order order = paymentService.createOrder(user, request.getTargetPlan(), request.getAmountInCents());

        // 2. Calculamos la firma de integridad obligatoria para Wompi
        String integritySignature = paymentService.generateIntegritySignature(
                order.getReference(),
                order.getAmountInCents(),
                order.getCurrency()
        );

        // 3. Empacamos todo hacia el frontend
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getId());
        response.setReference(order.getReference());
        response.setAmountInCents(order.getAmountInCents());
        response.setCurrency(order.getCurrency());
        response.setIntegritySignature(integritySignature); // <-- Clave para el widget

        return ResponseEntity.ok(response);
    }

    /**
     * ENDPOINT 2: Recibir notificaciones en tiempo real desde Wompi
     * ACCESO: PÚBLICO (Debes excluirlo en tu SecurityFilterChain)
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWompiWebhook(@RequestBody WompiWebhookPayload payload) {
        try {
            // Extraemos la información anidada del payload de Wompi
            var transaction = payload.getData().getTransaction();

            // Wompi no manda el Hash directamente plano en raíces básicas, usualmente viene en la firma (signature)
            // que calcula uniendo propiedades + marcas de tiempo.
            paymentService.processWebhook(
                    transaction.getReference(),
                    transaction.getStatus(),
                    payload.getSignature().getChecksum(), // Firma enviada por Wompi
                    payload.getTimestamp().toString()     // Timestamp del evento
            );

            // Respondemos 200 OK estricto para que Wompi sepa que procesamos el evento y no reintente
            return ResponseEntity.ok("Event processed successfully");

        } catch (SecurityException e) {
            // Logear intento de fraude pero responder con estatus controlado
            System.err.println("ALERTA DE SEGURIDAD WEBHOOK: " + e.getMessage());
            return ResponseEntity.status(401).body("Invalid signature");
        } catch (Exception e) {
            System.err.println("Error procesando Webhook: " + e.getMessage());
            return ResponseEntity.status(500).body("Internal error");
        }
    }
    /**
     * ENDPOINT 3: Consulta rápida de estatus para la pantalla de confirmación
     * ACCESO: Autenticado vía JWT
     */
    @GetMapping("/order-summary/{reference}")
    public ResponseEntity<OrderSummaryResponse> getOrderSummary(@PathVariable String reference) {
        // Buscamos la orden por su referencia única empresarial
        Order order = orderRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Transacción no registrada"));

        // Mapeamos los detalles limpios de la compra para que la UI los pinte amigablemente
        OrderSummaryResponse summary = new OrderSummaryResponse();
        summary.setReference(order.getReference());
        summary.setAmountInCents(order.getAmountInCents());
        summary.setCurrency(order.getCurrency());
        summary.setStatus(order.getStatus()); // "PENDING", "APPROVED", etc.
        summary.setTargetPlan(order.getTargetPlan().toString());
        summary.setOrganizationName(order.getOrganization().getName());

        return ResponseEntity.ok(summary);
    }
}

// ==========================================
// DTOs PARA PETICIÓN DE CHECKOUT (INTERNO)
// ==========================================

@Data
class CheckoutRequest {
    private PlanType targetPlan;
    private Long amountInCents;
}

@Data
class OrderResponse {
    private Long orderId;
    private String reference;
    private Long amountInCents;
    private String currency;

    public void setIntegritySignature(String integritySignature) {
    }
}

// ==========================================
// DTOs PARA MAPEAR EL WEBHOOK DE WOMPI (EXTERNO)
// ==========================================

@Data
class WompiWebhookPayload {
    private String event;
    private WompiData data;
    private WompiSignature signature;
    private Long timestamp;
    private String environment;
}

@Data
class WompiData {
    private WompiTransaction transaction;
}

@Data
class WompiTransaction {
    private String id;
    private String reference;
    private Long amount_in_cents;
    private String currency;
    private String status;
    private String payment_method_type;

    // Métodos helper rápidos para simplificar el mapeo a tipos estándar de Java

    public String getReference() { return reference; }
    public String getStatus() { return status; }
    public Long getAmountInCents() { return amount_in_cents; }
}

@Data
class WompiSignature {
    private String checksum; // El hash SHA-256 enviado para verificar autoría
    private String properties;
}

@Data
class OrderSummaryResponse {
    private String reference;
    private Long amountInCents;
    private String currency;
    private String status;
    private String targetPlan;
    private String organizationName;
}