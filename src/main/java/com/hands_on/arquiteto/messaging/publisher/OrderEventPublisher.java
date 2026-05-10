package com.hands_on.arquiteto.messaging.publisher;

import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import com.hands_on.arquiteto.config.RabbitConfig;
import com.hands_on.arquiteto.entity.Order;
import com.hands_on.arquiteto.messaging.payload.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * ============= 📤 ORDER EVENT PUBLISHER — PUBLICADOR DE EVENTOS DE DOMÍNIO =============
 *
 * 🧠 VISÃO GERAL:
 *
 * Este componente é responsável por publicar eventos no RabbitMQ, representando mudanças relevantes
 * no estado do sistema.
 *
 * 👉 Atua como PRODUTOR (Producer) no modelo de mensageria.
 *
 * -------------- 🎯 RESPONSABILIDADE -------------
 *
 * ✅ Converter entidade de domínio (Order) em evento de negócio ✅ Publicar o evento no RabbitMQ ✅
 * Garantir desacoplamento entre componentes
 *
 * ----------- 📡 CONTEXTO ARQUITETURAL -----------
 *
 * Controller ↓ Service (OrderService) ↓ OrderEventPublisher (este componente) ↓ RabbitMQ (Exchange
 * → Queue) ↓ Consumers (Payment, Email, etc.)
 *
 * ------------- 🔗 OBSERVABILIDADE — CORRELATION ID ------------
 *
 * Este componente recupera o correlationId do MDC:
 *
 * 👉 MDC.get("correlationId")
 *
 * E o inclui no evento enviado ao RabbitMQ.
 *
 * ✅ Isso permite: - rastrear o evento entre serviços - correlacionar logs distribuídos - debugar o
 * fluxo completo (HTTP → MQ → Consumer)
 *
 * ⚠️ IMPORTANTE: O MDC NÃO é propagado automaticamente entre serviços.
 *
 * 👉 Portanto, precisamos colocar o correlationId dentro do evento.
 *
 * --------------- ⚠️ PRINCÍPIO IMPORTANTE ------------
 *
 * ❌ NÃO enviar entidades (Order) ✅ SEMPRE enviar eventos de domínio (OrderCreatedEvent)
 *
 * Benefícios:
 *
 * ✔ baixo acoplamento ✔ evolução independente dos serviços ✔ clareza semântica (evento representa
 * um fato de negócio)
 *
 */
@Slf4j
@Service
public class OrderEventPublisher {

    /**
     * =============== 📤 RABBIT TEMPLATE =============
     *
     * Componente do Spring responsável por enviar mensagens ao RabbitMQ.
     *
     * Funções: - Converter objeto Java → JSON automaticamente - Enviar para exchange com routing
     * key
     *
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 🔧 Injeção de dependência via construtor
     */
    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * ============== 📡 PUBLICAÇÃO DE EVENTO: ORDER CREATED ==============
     *
     * 🧠 Este método representa o evento de negócio:
     *
     * 👉 "Um pedido foi criado"
     *
     * ------------- 🔁 FLUXO EXECUTADO ------------
     *
     * 1. Recupera correlationId do MDC 2. Converte entidade Order → OrderCreatedEvent 3. Publica
     * evento no RabbitMQ
     *
     * -------------- 📦 ESTRUTURA DO EVENTO --------------
     *
     * O evento contém:
     *
     * - orderId → identificação do pedido - amount → valor do pedido - correlationId → rastreamento
     * distribuído
     *
     * -------------- 🎯 RESULTADO --------------
     *
     * Outros serviços podem reagir:
     *
     * - PaymentConsumer → processa pagamento - EmailConsumer → envia notificação
     *
     * ----------------⚠️ BOAS PRÁTICAS ------------
     *
     * ✅ Publicar apenas após persistência no banco ✅ Incluir correlationId para observabilidade ✅
     * Evitar lógica de negócio no publisher
     *
     */
    public void publish(Order order) {

        // 🔗 Recupera correlationId do contexto atual (MDC)
        String correlationId = MDC.get("correlationId");

        // 📦 Cria evento de domínio com dados necessários (desacoplado da entidade)
        OrderCreatedEvent event =
                new OrderCreatedEvent(order.getId(), order.getAmount(), correlationId);

        // 🪵 Log estruturado para rastreamento da publicação
        log.info("Publicando evento OrderCreated | orderId={}", order.getId());

        // 📡 Envia mensagem para o RabbitMQ
        rabbitTemplate.convertAndSend(RabbitConfig.ORDER_EXCHANGE, // exchange
                RabbitConfig.ORDER_CREATED, // routing key
                event // payload (evento)
        );
    }
}
