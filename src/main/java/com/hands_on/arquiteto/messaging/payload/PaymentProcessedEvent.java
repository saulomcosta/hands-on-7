package com.hands_on.arquiteto.messaging.payload;

import java.util.UUID;

/**
 * ========= 💳 EVENTO DE DOMÍNIO: PAYMENT PROCESSED =========
 *
 * 🧠 VISÃO GERAL:
 *
 * Este evento representa um fato de negócio que já aconteceu no sistema:
 *
 * 👉 "O pagamento de um pedido foi processado com sucesso"
 *
 * ----------- ⚠️ IMPORTANTE:
 *
 * Este NÃO é: ❌ entidade de banco (JPA) ❌ DTO de API (HTTP/REST)
 *
 * ✅ É um EVENTO DE DOMÍNIO
 *
 * 👉 utilizado exclusivamente para comunicação assíncrona entre serviços em uma arquitetura
 * orientada a eventos (Event-Driven Architecture - EDA)
 *
 * ----------- 🎯 OBJETIVO ----------
 *
 * Permitir que outros componentes reajam ao evento de pagamento processado, sem acoplamento direto
 * ao PaymentConsumer.
 *
 * Exemplo:
 *
 * PaymentProcessedEvent ↓ EmailConsumer → envia e-mail ↓ NotificationService → envia push / SMS
 *
 * 👉 novos consumidores podem ser adicionados sem alterar o fluxo existente
 *
 * ------------ 📡 CONTEXTO NO FLUXO (EVENT-DRIVEN) ------------
 *
 * OrderCreatedEvent ↓ PaymentConsumer ↓ ✅ PaymentProcessedEvent (este evento) ↓ EmailConsumer
 *
 * 👉 Cada evento representa um estado do processo de negócio
 *
 * ------------- 🔗 OBSERVABILIDADE — CORRELATION ID ------------
 *
 * Este evento inclui um campo:
 *
 * 👉 correlationId
 *
 * ✅ Função: - rastrear o fluxo ponta-a-ponta - correlacionar logs entre serviços - permitir debug
 * distribuído
 *
 * Exemplo de rastreamento:
 *
 * [correlationId=abc-123] Pedido criado [correlationId=abc-123] Pagamento processado
 * [correlationId=abc-123] Email enviado
 *
 * ⚠️ IMPORTANTE:
 *
 * O MDC (Mapped Diagnostic Context): ❌ NÃO é propagado automaticamente entre serviços
 *
 * 👉 Por isso: ✅ O correlationId PRECISA viajar dentro do evento
 *
 * -------------- 📦 ESTRUTURA DO EVENTO -----------
 *
 * Este evento deve conter apenas os dados necessários:
 *
 * ✅ orderId → identifica o pedido ✅ correlationId → rastreamento distribuído
 *
 * ❌ NÃO incluir: - entidade completa (Order) - dados desnecessários - lógica de negócio
 *
 * ---------- 🔒 IDEMPOTÊNCIA (CRÍTICO EM PRODUÇÃO) ----------
 *
 * Consumidores deste evento devem garantir:
 *
 * ✅ o mesmo evento NÃO pode gerar efeitos duplicados
 *
 * Exemplo: - e-mail não pode ser enviado duas vezes - pagamento não pode ser processado duas vezes
 *
 * 👉 Estratégia: - verificar estado no banco antes de executar
 *
 * --------------- 📦 POR QUE USAR RECORD? -----------
 *
 * ✅ Imutável → não pode ser alterado após criação ✅ Thread-safe → seguro em ambientes concorrentes
 * ✅ Menos código → sem getters/setters
 *
 * 👉 Ideal para eventos em mensageria
 *
 * --------------- 🚀 EVOLUÇÃO FUTURA (PRODUÇÃO REAL) ---------------
 *
 * Este evento pode ser estendido com:
 *
 * - Instant createdAt → timestamp - String version → versionamento - String source → origem do
 * evento
 *
 * Exemplo:
 *
 * public record PaymentProcessedEvent( UUID orderId, String correlationId, Instant createdAt,
 * String version ) {}
 *
 */
public record PaymentProcessedEvent(

        /**
         * 🆔 ID DO PEDIDO
         *
         * - Identificador único do pedido - Usado para correlacionar eventos ao longo do fluxo -
         * Permite buscar dados no banco
         */
        UUID orderId,

        /**
         * 🔗 CORRELATION ID
         *
         * - Identificador da requisição original - Permite rastrear o fluxo completo entre serviços
         *
         * Exemplo: HTTP → Service → RabbitMQ → Consumer → Email
         *
         * 👉 SEM esse campo: ❌ não é possível correlacionar logs ❌ debugging em produção fica
         * extremamente difícil
         */
        String correlationId

) {
}
