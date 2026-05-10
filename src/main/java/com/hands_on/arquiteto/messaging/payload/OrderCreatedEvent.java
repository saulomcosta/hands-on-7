package com.hands_on.arquiteto.messaging.payload;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ========= 📦 EVENTO DE DOMÍNIO: ORDER CREATED =========
 *
 * 🧠 VISÃO GERAL:
 *
 * Este evento representa um fato de negócio já ocorrido:
 *
 * 👉 "Um pedido foi criado no sistema"
 *
 * -------- ⚠️ IMPORTANTE:
 *
 * Este NÃO é: ❌ entidade de banco (JPA) ❌ DTO de API HTTP
 *
 * ✅ É um EVENTO DE DOMÍNIO
 *
 * 👉 Usado para comunicação assíncrona entre serviços em uma arquitetura orientada a eventos (EDA)
 *
 * ------------ 🎯 OBJETIVO ------------
 *
 * Permitir que outros serviços reajam à criação de um pedido, sem depender diretamente do
 * OrderService.
 *
 * Exemplo de fluxo:
 *
 * OrderCreatedEvent ↓ PaymentConsumer → processa pagamento ↓ PaymentProcessedEvent ↓ EmailConsumer
 * → envia notificação
 *
 * 👉 Novos consumidores podem ser adicionados sem alterar o sistema atual
 *
 * ------------ 📡 CONTEXTO NO FLUXO (EVENT-DRIVEN) -----------
 *
 * Controller ↓ OrderService ↓ ✅ OrderCreatedEvent (este evento) ↓ PaymentConsumer
 *
 * 👉 Esse evento inicia o pipeline assíncrono de processamento do pedido
 *
 * ------------ 🔗 OBSERVABILIDADE — CORRELATION ID ------------
 *
 * Este evento possui o campo:
 *
 * 👉 correlationId
 *
 * ✅ Função: - rastrear o fluxo ponta-a-ponta - correlacionar logs entre serviços - permitir debug
 * distribuído
 *
 * Exemplo:
 *
 * [correlationId=abc-123] Pedido criado [correlationId=abc-123] Pagamento iniciado
 * [correlationId=abc-123] Email enviado
 *
 * ⚠️ IMPORTANTE:
 *
 * O MDC (Mapped Diagnostic Context): ❌ NÃO é propagado automaticamente entre serviços
 *
 * 👉 Portanto: ✅ O correlationId PRECISA ser enviado dentro do evento
 *
 * ------------- 📦 ESTRUTURA DO EVENTO ----------
 *
 * Este evento contém apenas os dados necessários:
 *
 * ✅ orderId → identificação do pedido ✅ amount → valor para processamento financeiro ✅
 * correlationId → rastreamento do fluxo
 *
 * ❌ NÃO incluir: - entidade completa (Order) - lógica de negócio - dados desnecessários
 *
 * 👉 Isso mantém: ✔ baixo acoplamento ✔ eventos leves
 *
 * -------------- 🔒 IDEMPOTÊNCIA (CRÍTICO) ----------
 *
 * Consumidores deste evento devem garantir:
 *
 * ✅ o mesmo evento NÃO pode ser processado mais de uma vez
 *
 * Exemplo: - não cobrar o pagamento duas vezes
 *
 * 👉 Estratégia: - verificar status no banco antes de processar
 *
 * --------------📦 POR QUE USAR RECORD? -----------
 *
 * ✅ Imutável → não pode ser alterado ✅ Thread-safe → seguro em concorrência ✅ Simples → menos
 * código boilerplate
 *
 * 👉 Ideal para eventos de mensageria
 *
 * ---------------- 🚀 EVOLUÇÃO FUTURA (PRODUÇÃO REAL) ---------------
 *
 * Este evento pode evoluir para incluir:
 *
 * - Instant createdAt → timestamp do evento - String version → controle de versão - String source →
 * origem do evento
 *
 * Exemplo:
 *
 * public record OrderCreatedEvent( UUID orderId, BigDecimal amount, String correlationId, Instant
 * createdAt, String version ) {}
 *
 */
public record OrderCreatedEvent(

        /**
         * 🆔 ID DO PEDIDO
         *
         * - Identificador único do pedido - Permite rastrear o pedido no banco - Usado pelos
         * consumidores para correlacionar operações
         */
        UUID orderId,

        /**
         * 💰 VALOR DO PEDIDO
         *
         * - Necessário para processamento financeiro - Utilizado pelo PaymentConsumer
         *
         * ⚠️ Uso de BigDecimal evita erros de precisão
         */
        BigDecimal amount,

        /**
         * 🔗 CORRELATION ID
         *
         * - Identificador da requisição original - Permite rastrear todo o fluxo distribuído
         *
         * Exemplo: HTTP → Service → RabbitMQ → Consumer
         *
         * 👉 SEM esse campo: ❌ impossível correlacionar logs ❌ difícil debugar produção
         */
        String correlationId

) {
}
