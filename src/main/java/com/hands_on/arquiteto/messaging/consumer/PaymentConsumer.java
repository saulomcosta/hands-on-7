package com.hands_on.arquiteto.messaging.consumer;

import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import com.hands_on.arquiteto.config.RabbitConfig;
import com.hands_on.arquiteto.messaging.payload.OrderCreatedEvent;
import com.hands_on.arquiteto.messaging.payload.PaymentProcessedEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * ============= 📥 PAYMENT CONSUMER — PROCESSADOR DE PAGAMENTO =============
 *
 * 🧠 VISÃO GERAL:
 *
 * Este componente consome eventos OrderCreatedEvent e executa o processamento de pagamento de forma
 * assíncrona.
 *
 * 👉 Atua como CONSUMER em uma arquitetura orientada a eventos (EDA)
 *
 * ------------- 📡 FLUXO DO SISTEMA -----------
 *
 * OrderCreatedEvent ↓ RabbitMQ (payment.queue) ↓ PaymentConsumer (este componente) ↓
 * PaymentProcessedEvent ↓ EmailConsumer
 *
 * ------------ 🎯 RESPONSABILIDADES ------------
 *
 * ✅ Consumir OrderCreatedEvent ✅ Processar pagamento ✅ Publicar PaymentProcessedEvent ✅ Garantir
 * rastreabilidade (correlationId)
 *
 * ------------- 🔗 OBSERVABILIDADE (MDC) --------------
 *
 * O correlationId vem no evento.
 *
 * 👉 Aqui: - recuperamos - colocamos no MDC
 *
 * ✅ Isso garante: - rastreamento ponta-a-ponta - logs correlacionados
 *
 */
@Slf4j
@Service
public class PaymentConsumer {

    /**
     * 📤 Template usado para publicar novos eventos no RabbitMQ
     */
    private final RabbitTemplate rabbitTemplate;

    public PaymentConsumer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * ============== 📥 CONSUMIDOR PRINCIPAL — ORDER CREATED EVENT ==============
     *
     * 👉 Recebe evento: "Pedido foi criado"
     *
     * 🔁 Fluxo:
     *
     * 1. Coloca correlationId no MDC 2. Processa pagamento 3. Publica PaymentProcessedEvent
     *
     * ------------- 🚀 CONCURRENCY ---------------
     *
     * concurrency = "5-20"
     *
     * - mínimo: 5 consumers - máximo: 20 consumers (auto scaling)
     *
     * 👉 aumenta throughput sob carga
     *
     */
    @RabbitListener(queues = RabbitConfig.PAYMENT_QUEUE, concurrency = "5-20")
    public void process(OrderCreatedEvent event) {

        // 🔗 1. Propaga correlationId para o contexto do log
        MDC.put("correlationId", event.correlationId());

        try {
            log.info("Iniciando processamento de pagamento | orderId={}", event.orderId());

            // 💳 2. Processa pagamento
            processPayment(event);

            // 📦 3. Cria próximo evento do fluxo
            PaymentProcessedEvent nextEvent =
                    new PaymentProcessedEvent(event.orderId(), event.correlationId());

            // 📡 4. Publica evento
            rabbitTemplate.convertAndSend(RabbitConfig.PAYMENT_EXCHANGE,
                    RabbitConfig.PAYMENT_PROCESSED, nextEvent);

            log.info("Pagamento concluído e evento publicado | orderId={}", event.orderId());

        } catch (Exception e) {

            // ✅ log estruturado com erro
            log.error("Erro ao processar pagamento | orderId={}", event.orderId(), e);

            // ❗ permite retry / DLQ
            throw e;

        } finally {
            // 🧹 evita vazamento de contexto
            MDC.clear();
        }
    }

    /**
     * ============= 💳 PROCESSAMENTO DE PAGAMENTO =============
     *
     * 👉 Simula lógica de pagamento
     *
     * ⚠️ PRODUÇÃO: - gateway de pagamento - antifraude - atualização do banco
     *
     * ------------ 🔒 IDEMPOTÊNCIA ------------
     *
     * ESTE MÉTODO DEVE SER IDEMPOTENTE:
     *
     * ✅ não pode cobrar duas vezes
     *
     */
    private void processPayment(OrderCreatedEvent event) {

        try {
            log.info("Processando pagamento | orderId={}", event.orderId());

            // 🔄 simulação de latência
            Thread.sleep(200);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Pagamento processado | orderId={}", event.orderId());
    }

    /**
     * ============= ☠️ CONSUMIDOR DE ERRO (DLQ) ============
     *
     * 👉 Recebe mensagens que falharam definitivamente
     *
     * Fluxo: payment.queue → retry → DLQ
     *
     */
    @RabbitListener(queues = RabbitConfig.PAYMENT_DLQ)
    public void handleError(OrderCreatedEvent event) {

        // 🔗 manter rastreabilidade também na DLQ
        MDC.put("correlationId", event.correlationId());

        try {
            log.error("Mensagem enviada para DLQ | orderId={}", event.orderId());

            // 👉 aqui você implementaria:
            // - salvar erro em banco
            // - alertar equipe
            // - reprocessamento manual

        } finally {
            MDC.clear();
        }
    }
}

