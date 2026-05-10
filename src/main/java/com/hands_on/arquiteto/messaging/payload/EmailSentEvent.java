package com.hands_on.arquiteto.messaging.payload;

import java.util.UUID;

/**
 * =========== 📧 EVENTO DE DOMÍNIO: EMAIL SENT ===========
 *
 * 🧠 VISÃO GERAL:
 *
 * Este evento representa um fato de negócio concluído no sistema:
 *
 * 👉 "O e-mail de notificação do pedido foi enviado com sucesso"
 *
 * ------------ ⚠️ IMPORTANTE ------------
 *
 * Este NÃO é:
 *
 * ❌ Uma entidade de banco de dados (JPA) ❌ Um DTO de API (HTTP/REST)
 *
 * ✅ É um EVENTO DE DOMÍNIO
 *
 * 👉 utilizado exclusivamente para comunicação assíncrona entre serviços em uma arquitetura
 * orientada a eventos (EDA)
 *
 * --------------- 🎯 OBJETIVO --------------
 *
 * Permitir que outros componentes reajam ao envio do e-mail, sem acoplamento direto com o
 * EmailConsumer.
 *
 * Exemplos:
 *
 * EmailSentEvent ↓ AuditService → registra o envio ↓ AnalyticsService → coleta métricas ↓
 * CRMService → atualiza histórico do cliente
 *
 * 👉 Novos consumidores podem ser adicionados sem alterar código existente
 *
 * ------------- 📡 CONTEXTO NO FLUXO (EVENT-DRIVEN) -------------
 *
 * OrderCreatedEvent ↓ PaymentProcessedEvent ↓ EmailConsumer ↓ ✅ EmailSentEvent (este evento)
 *
 * 👉 Representa a etapa FINAL do fluxo de notificação
 *
 * ------------- 🔗 OBSERVABILIDADE — CORRELATION ID -------------
 *
 * Este evento contém:
 *
 * 👉 correlationId
 *
 * ✅ Função:
 *
 * - rastrear o fluxo ponta-a-ponta - correlacionar logs entre serviços - facilitar debugging em
 * sistemas distribuídos
 *
 * Exemplo de rastreamento:
 *
 * [correlationId=abc-123] Pedido criado [correlationId=abc-123] Pagamento processado
 * [correlationId=abc-123] Email enviado
 *
 * ⚠️ IMPORTANTE:
 *
 * O MDC (Mapped Diagnostic Context): ❌ NÃO atravessa serviços automaticamente
 *
 * 👉 Portanto: ✅ o correlationId DEVE ser transportado dentro do evento
 *
 * ----------- 📦 ESTRUTURA DO EVENTO -----------
 *
 * Este evento é propositalmente enxuto e contém apenas:
 *
 * ✅ orderId → identifica o pedido ✅ correlationId → rastreio distribuído
 *
 * ❌ NÃO incluir:
 *
 * - entidade completa (Order) - lógica de negócio - dados desnecessários
 *
 * 👉 Isso garante baixo acoplamento e melhor performance
 *
 * ------------ 🔒 IDEMPOTÊNCIA (CRÍTICO EM PRODUÇÃO) ------------
 *
 * Consumidores deste evento devem garantir:
 *
 * ✅ O mesmo evento NÃO deve gerar efeito duplicado
 *
 * Exemplos:
 *
 * ❌ Não registrar envio duplicado ❌ Não disparar múltiplas notificações externas
 *
 * 👉 Estratégia comum:
 *
 * - verificar se já foi processado antes de executar ação
 *
 * ----------📦 POR QUE USAR RECORD? ----------
 *
 * ✅ Imutável → não pode ser alterado após criação ✅ Thread-safe → seguro em ambientes concorrentes
 * ✅ Simples → menos código boilerplate
 *
 * 👉 Ideal para eventos em sistemas distribuídos
 *
 * -------------- 🚀 EVOLUÇÃO FUTURA (PRODUÇÃO REAL) ---------------
 *
 * Este evento pode evoluir com:
 *
 * - Instant sentAt → timestamp do envio - String status → SUCCESS / FAILURE - String version →
 * controle de versão do evento
 *
 * Exemplo:
 *
 * public record EmailSentEvent( UUID orderId, String correlationId, Instant sentAt, String status )
 * {}
 *
 */
public record EmailSentEvent(

        /**
         * 🆔 ID DO PEDIDO
         *
         * - Identificador único do pedido - Permite correlacionar este evento com outros eventos do
         * fluxo
         */
        UUID orderId,

        /**
         * 🔗 CORRELATION ID
         *
         * - Identificador da requisição original - Permite rastrear o fluxo completo entre serviços
         *
         * Exemplo: HTTP → Service → RabbitMQ → Consumer → Email
         *
         * 👉 Sem esse campo: ❌ não é possível rastrear a jornada do pedido ❌ debugging em produção
         * torna-se extremamente difícil
         */
        String correlationId

) {
}
