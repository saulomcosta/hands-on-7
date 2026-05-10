package com.hands_on.arquiteto.controller;

// 📦 Importações básicas de tipos
import java.math.BigDecimal;
import java.util.UUID;
// 📊 MDC (Mapped Diagnostic Context) → permite adicionar dados ao contexto do log (ex:
// correlationId)
import org.slf4j.MDC;
// 🌐 Anotações do Spring para expor endpoints REST
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
// 📦 DTO e entidades da aplicação
import com.hands_on.arquiteto.dto.OrderRequestDto;
import com.hands_on.arquiteto.entity.Order;
// 🧠 Camada de serviço (regra de negócio)
import com.hands_on.arquiteto.service.OrderService;
// 🪵 Logger automatizado do Lombok (log.info, log.error, etc.)
import lombok.extern.slf4j.Slf4j;


/**
 * ========== 🌐 CAMADA: CONTROLLER (Entrada HTTP / API REST) ==========
 *
 * 🧠 RESPONSABILIDADE PRINCIPAL:
 *
 * - Receber requisições HTTP - Fazer binding/conversão de parâmetros - Delegar processamento para a
 * camada de Service - Retornar resposta ao cliente
 *
 * ❗ IMPORTANTE: - NÃO deve conter regra de negócio - NÃO deve acessar banco diretamente
 *
 * ✔ Atua como "porta de entrada" do sistema
 *
 * ---------- 🔗 CONCEITO AVANÇADO APLICADO AQUI: CORRELATION ID ----------
 *
 * Cada requisição recebe um identificador único (UUID), utilizado para rastrear todo o fluxo da
 * requisição no sistema distribuído.
 *
 * Exemplo:
 *
 * [correlationId=abc-123] Pedido criado [correlationId=abc-123] Pagamento processado
 * [correlationId=abc-123] Email enviado
 *
 * 👉 Isso permite: - Debug facilitado - Observabilidade - Rastreio ponta-a-ponta
 *
 */
@Slf4j // 👉 Habilita logger automático (log.info, log.error, etc.)
@RestController // 👉 Define classe como controller REST (retorna JSON)
@RequestMapping("/orders") // 👉 Prefixo base de todos os endpoints
public class OrderController {

    /**
     * ========= 🧠 DEPENDÊNCIA DA CAMADA DE SERVIÇO =========
     *
     * 👉 Responsável por executar a regra de negócio
     *
     * ✔ Injeção via construtor (boa prática) ✔ Facilita testes ✔ Reduz acoplamento
     */
    private final OrderService orderService;

    /**
     * 🔧 Construtor com injeção automática do Spring
     */
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * ============= 📨 ENDPOINT: CRIAÇÃO DE PEDIDO =============
     *
     * POST /orders?amount=100
     *
     * 🔁 FLUXO:
     *
     * 1. Gera correlationId (UUID) 2. Coloca no MDC (contexto de log) 3. Registra log de entrada 4.
     * Chama Service 5. Registra log de saída 6. Limpa o MDC
     *
     * ----------- ⚠️ IMPORTANTE:
     *
     * O MDC é baseado em Thread.
     *
     * 👉 SEMPRE limpar após uso (MDC.clear()) 👉 Evita vazamento de contexto entre requisições
     *
     */
    @PostMapping
    public Order createOrder(@RequestParam BigDecimal amount) {

        // 🔑 1. Gera um identificador único para rastrear toda a requisição
        String correlationId = UUID.randomUUID().toString();

        // 📊 2. Coloca o correlationId no contexto de log (MDC)
        // Isso automaticamente será incluído nos logs (se configurado)
        MDC.put("correlationId", correlationId);

        try {

            // 🪵 3. Log de entrada (requisição recebida)
            log.info("Recebendo requisição de criação de pedido | amount={}", amount);

            // 🧠 4. Chamada para camada de negócio
            Order order = orderService.createOrder(amount);

            // 🪵 5. Log de sucesso com identificação do pedido
            log.info("Pedido criado com sucesso | orderId={}", order.getId());

            // 📤 6. Retorna resposta para o cliente (Spring converte para JSON)
            return order;

        } finally {

            // 🧹 7. Limpa o MDC para evitar vazamento entre threads
            MDC.clear();
        }
    }

    /**
     * =========== 🚀 ENDPOINT: STRESS TEST (TESTE DE CARGA) ==========
     *
     * POST /orders/stress
     *
     * 🧠 OBJETIVO:
     *
     * Simular alta carga criando 1000 pedidos rapidamente, testando comportamento do sistema em
     * cenários reais.
     *
     * ----------- 🔁 FLUXO:
     *
     * Para cada iteração: 1. Cria um novo correlationId 2. Coloca no MDC 3. Cria pedido 4. Limpa
     * contexto
     *
     * ----------- 🎯 O QUE ISSO PERMITE OBSERVAR:
     *
     * ✅ Performance do sistema ✅ Comportamento do RabbitMQ ✅ Backpressure (fila crescendo) ✅
     * Escalabilidade dos consumers ✅ Logs concorrentes com rastreamento
     *
     * ----------- ⚠️ IMPORTANTE:
     *
     * Cada pedido tem seu próprio correlationId.
     *
     * 👉 Isso simula múltiplos usuários reais no sistema.
     *
     */
    @PostMapping("/stress")
    public String stressTest() {

        // 🔁 Loop de 1000 requisições simuladas
        for (int i = 0; i < 1000; i++) {

            // 🔑 1. Cria correlationId único para cada pedido
            String correlationId = UUID.randomUUID().toString();

            // 📊 2. Define no contexto de log
            MDC.put("correlationId", correlationId);

            try {

                // 📦 3. Cria DTO de entrada (simula payload)
                OrderRequestDto request = new OrderRequestDto();
                request.setAmount(BigDecimal.valueOf(100.0));

                // 🪵 4. Log indicando envio de pedido
                log.info("Disparando pedido em massa");

                // 🧠 5. Executa lógica de criação
                orderService.createOrder(request.getAmount());

            } finally {

                // 🧹 6. Limpa contexto após cada requisição
                MDC.clear();
            }
        }

        // 📤 Resposta simples ao cliente
        return "1000 pedidos enviados!";
    }
}
