package com.hands_on.arquiteto.service;

// 📦 Tipos básicos usados no domínio financeiro
import java.math.BigDecimal;
// Marca a classe como componente de negócio (Service)
import org.springframework.stereotype.Service;
// 📦 Entidade de domínio (representa o pedido no sistema)
import com.hands_on.arquiteto.entity.Order;
// 📡 Componente responsável por publicar eventos no RabbitMQ
import com.hands_on.arquiteto.messaging.publisher.OrderEventPublisher;
// 🗄️ Repositório JPA (acesso ao banco de dados)
import com.hands_on.arquiteto.repository.OrderRepository;
// 🔄 Controle de transação (commit/rollback automático)
import jakarta.transaction.Transactional;
// 🪵 Logger estruturado (log.info, log.error...)
import lombok.extern.slf4j.Slf4j;


/**
 * ========== 🧠 CAMADA: SERVICE (Regra de Negócio / Domínio) ==========
 *
 * 🎯 RESPONSABILIDADE:
 *
 * - Orquestrar o fluxo de criação de pedidos - Persistir dados no banco - Publicar eventos no
 * RabbitMQ
 *
 * ---------- 🔗 OBSERVABILIDADE (MDC + correlationId) ----------
 *
 * Este serviço NÃO cria o correlationId.
 *
 * 👉 Ele apenas CONSOME o correlationId criado no Controller via MDC:
 *
 * MDC.put("correlationId", "abc-123")
 *
 * ✅ Isso garante: - rastreamento ponta-a-ponta - logs correlacionados entre camadas
 *
 * ---------- ⚠️ IMPORTANTE:
 *
 * O MDC funciona por thread → não deve ser alterado aqui
 *
 */
@Slf4j
@Service
public class OrderService {

    /**
     * ========== 🗄️ REPOSITÓRIO (Persistência) ==========
     *
     * 👉 Responsável por salvar e consultar dados no banco 👉 Usa Spring Data JPA (abstrai SQL)
     */
    private final OrderRepository orderRepository;

    /**
     * ============= 📡 PUBLISHER (Integração com RabbitMQ) =============
     *
     * 👉 Responsável por enviar eventos para o broker 👉 Permite arquitetura orientada a eventos
     * (EDA)
     */
    private final OrderEventPublisher orderPublisher;

    /**
     * =========== 🔧 CONSTRUTOR (Injeção de Dependência) ==========
     *
     * O Spring injeta automaticamente:
     *
     * ✔ OrderRepository ✔ OrderEventPublisher
     *
     * ✅ Boa prática: - Imutabilidade - Testabilidade
     */
    public OrderService(OrderRepository orderRepository, OrderEventPublisher orderPublisher) {
        this.orderRepository = orderRepository;
        this.orderPublisher = orderPublisher;
    }

    /**
     * ============= 🧩 CASO DE USO: CRIAR PEDIDO ============
     *
     * 🔁 FLUXO COMPLETO:
     *
     * 1. Recupera correlationId do MDC 2. Cria entidade Order em memória 3. Persiste no banco 4.
     * Publica evento no RabbitMQ 5. Retorna resultado
     *
     * ---------------- 💡 TRANSAÇÃO
     *
     * @Transactional:
     *
     *                 - Garante consistência no banco - Se falhar → rollback automático
     *
     *                 ⚠️ IMPORTANTE: A publicação no RabbitMQ NÃO faz parte da transação
     *
     *                 👉 risco: - salvar no banco e falhar no evento
     *
     *                 ✔ solução real: - Outbox Pattern (produção)
     *
     */
    @Transactional
    public Order createOrder(BigDecimal amount) {

        // 🪵 Log de início do processamento
        log.info("Iniciando criação de pedido | amount={}", amount);

        /**
         * =========== 1. CRIAÇÃO DA ENTIDADE EM MEMÓRIA ===========
         *
         * 👉 Ainda NÃO está no banco 👉 Apenas um objeto Java
         *
         * status = CREATED → indica início do fluxo
         */
        Order order = Order.builder().amount(amount) // 💰 valor do pedido
                .status("CREATED") // 📊 estado inicial
                .build();

        /**
         * ============= 2. PERSISTÊNCIA NO BANCO =============
         *
         * 👉 Aqui ocorre: - execução do INSERT via Hibernate - geração automática do ID (UUID)
         *
         * ✅ Após isso: - pedido existe no banco
         */
        orderRepository.save(order);

        // 🪵 Log após persistência
        log.info("Pedido persistido com sucesso | orderId={}", order.getId());

        /**
         * ============ 3. PUBLICAÇÃO DE EVENTO ============
         *
         * 👉 Dispara evento: "OrderCreated"
         *
         * 👉 Outros serviços reagem: - PaymentConsumer → processa pagamento - EmailConsumer → envia
         * notificação
         *
         * ✅ Comunicação assíncrona ✅ Desacoplamento entre serviços
         */
        orderPublisher.publish(order);

        // 🪵 Log após publicação
        log.info("Evento OrderCreated publicado | orderId={}", order.getId());

        /**
         * ============= 4. RETORNO =============
         *
         * 👉 Retorna entidade persistida 👉 Já contém ID gerado
         */
        return order;
    }
}
