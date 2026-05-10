package com.hands_on.arquiteto.messaging.consumer;

import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import com.hands_on.arquiteto.config.RabbitConfig;
import com.hands_on.arquiteto.messaging.payload.PaymentProcessedEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * =========== 📧 EMAIL CONSUMER — PROCESSADOR DE NOTIFICAÇÕES ===========
 *
 * 🧠 VISÃO GERAL:
 *
 * Este componente consome eventos PaymentProcessedEvent e envia notificações por e-mail de forma
 * assíncrona.
 *
 * 👉 Atua como CONSUMER em uma arquitetura orientada a eventos (EDA)
 *
 * --------------- 📡 FLUXO DO SISTEMA ------------
 *
 * PaymentProcessedEvent ↓ RabbitMQ (email.queue) ↓ EmailConsumer (este componente) ↓ (opcional)
 * EmailSentEvent
 *
 * --------------- 🎯 RESPONSABILIDADES ---------------
 *
 * ✅ Consumir PaymentProcessedEvent ✅ Enviar e-mail ✅ Garantir rastreabilidade (correlationId) ✅
 * Evitar duplicidade (idempotência)
 *
 * --------------- 🔗 OBSERVABILIDADE (MDC) ---------------
 *
 * O correlationId vem no evento.
 *
 * 👉 Aqui: - restauramos no MDC - utilizamos automaticamente nos logs
 *
 * ✅ Permite: - rastreamento ponta-a-ponta - debug em produção
 *
 */
@Slf4j
@Service
public class EmailConsumer {

    /**
     * 📤 RabbitTemplate (opcional)
     *
     * Pode ser usado para publicar novos eventos ex: EmailSentEvent
     */
    private final RabbitTemplate rabbitTemplate;

    public EmailConsumer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * ===========📥 CONSUMIDOR PRINCIPAL — PAYMENT PROCESSED EVENT ===========
     *
     * 👉 Reage ao evento: "Pagamento processado"
     *
     * 🔁 Fluxo:
     *
     * 1. Restaura correlationId no MDC 2. Envia e-mail 3. Loga resultado
     *
     * ------------- 🚀 CONCURRENCY ---------------
     *
     * concurrency = "1-5"
     *
     * - mínimo: 1 consumer - máximo: 5 consumers
     *
     * 👉 adequado para tarefas leves (email)
     *
     */
    @RabbitListener(queues = RabbitConfig.EMAIL_QUEUE, concurrency = "1-5")
    public void sendEmail(PaymentProcessedEvent event) {

        // 🔗 1. Restaura correlationId para logs
        MDC.put("correlationId", event.correlationId());

        try {
            log.info("Iniciando envio de e-mail | orderId={}", event.orderId());

            // 📧 2. Executa envio
            send(event);

            log.info("E-mail enviado com sucesso | orderId={}", event.orderId());

        } catch (Exception e) {

            // ❗ Log estruturado de erro
            log.error("Erro ao enviar e-mail | orderId={}", event.orderId(), e);

            // mantém retry / DLQ
            throw e;

        } finally {
            // 🧹 evita vazamento de contexto entre threads
            MDC.clear();
        }
    }

    /**
     * =============== 📧 ENVIO DE E-MAIL =============
     *
     * 👉 Simula envio de e-mail
     *
     * Em produção: - SMTP / SendGrid / SES - templates dinâmicos - personalização
     *
     * ------------ 🔒 IDEMPOTÊNCIA ------------
     *
     * ESTE MÉTODO DEVE GARANTIR:
     *
     * ✅ não enviar e-mail duplicado
     *
     */
    private void send(PaymentProcessedEvent event) {

        try {
            log.info("Enviando e-mail | orderId={}", event.orderId());

            // simulação de latência
            Thread.sleep(200);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Envio concluído | orderId={}", event.orderId());
    }

    /**
     * ============ ☠️ CONSUMIDOR DE ERRO (DLQ) ===========
     *
     * 👉 Recebe mensagens que falharam após retries
     *
     * Fluxo: email.queue → retry → DLQ
     *
     */
    @RabbitListener(queues = RabbitConfig.PAYMENT_DLQ)
    public void handleError(PaymentProcessedEvent event) {

        // 🔗 mantém rastreabilidade mesmo em erro
        MDC.put("correlationId", event.correlationId());

        try {

            log.error("Falha definitiva no envio de e-mail | orderId={}", event.orderId());

            // 👉 aqui você poderia:
            // - salvar erro em banco
            // - enviar alerta
            // - permitir reprocessamento manual

        } finally {
            MDC.clear();
        }
    }
}
