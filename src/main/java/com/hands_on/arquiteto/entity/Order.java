package com.hands_on.arquiteto.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * =========== 📦 ENTITY: ORDER (PEDIDO) ============
 *
 * 🧠 VISÃO GERAL
 *
 * Esta classe representa o modelo persistido de um pedido no banco de dados.
 *
 * 👉 Ela é a "fonte da verdade" do sistema para pedidos (state store).
 *
 * -------------- 🎯 RESPONSABILIDADE
 * ---------------------------------------------------------------------------------
 *
 * ✅ Armazenar o estado do pedido ✅ Permitir rastrear o ciclo de vida (created → processed → etc) ✅
 * Servir de base para idempotência ✅ Permitir auditoria temporal ✅ Garantir consistência com
 * concorrência
 *
 * -------------- ⚠️ IMPORTANTE (ARQUITETURA) -------------
 *
 * Esta classe:
 *
 * ✅ Representa DADOS ❌ NÃO contém regra de negócio ❌ NÃO contém logs ❌ NÃO contém integração com
 * RabbitMQ
 *
 * 👉 Toda lógica deve existir em Service / Consumer
 *
 * -------------- 🔗 PAPEL NA OBSERVABILIDADE ---------------
 *
 * Mesmo sem logs, essa entidade permite responder:
 *
 * ✅ Quando foi criado? ✅ Quando foi atualizado? ✅ Qual o estado atual? ✅ Alguém sobrescreveu
 * concorrente?
 *
 * 👉 Ou seja: fornece HISTÓRICO e CONTEXTO do sistema
 *
 */
@Entity
@Table(name = "orders") // evita conflito com palavra reservada SQL "order"
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Order {

    // ============
    // 🔑 IDENTIFICADOR ÚNICO
    // ============

    /**
     * 🆔 ID DO PEDIDO
     *
     * - Tipo UUID → identificador global único
     *
     * ✅ Benefícios: - evita colisão entre serviços - ideal para sistemas distribuídos
     *
     * 👉 Impacto: ✔ base para idempotência ✔ usado em eventos e logs
     */
    @Id
    @GeneratedValue
    private UUID id;

    // =============
    // 💰 VALOR DO PEDIDO
    // =============

    /**
     * 💰 VALOR FINANCEIRO DO PEDIDO
     *
     * ✅ BigDecimal: - evita erro de precisão - obrigatório para sistemas financeiros
     *
     * ❌ NÃO usar double ou float
     *
     * 👉 Impacto: ✔ precisão financeira ✔ evita bugs críticos
     */
    private BigDecimal amount;

    // ===========
    // 📊 STATUS DO PEDIDO
    // ===========

    /**
     * 📊 STATUS DO PROCESSO
     *
     * Representa o estado do pedido no fluxo de negócio.
     *
     * Exemplos: - CREATED - PROCESSED - FAILED
     *
     * ---------------- 🔒 USO PARA IDEMPOTÊNCIA
     *
     * 👉 Antes de processar:
     *
     * if (status == PROCESSED) → NÃO executar novamente
     *
     * ------------- 👉 Impacto:
     *
     * ✔ evita duplicidade ✔ garante consistência ✔ controla fluxo do sistema
     */
    private String status;

    // ============
    // ⏱️ TIMESTAMP DE CRIAÇÃO (OBSERVABILIDADE)
    // ============

    /**
     * 🕒 DATA DE CRIAÇÃO
     *
     * 👉 Definido automaticamente no momento do INSERT
     *
     * ✔ Permite auditoria ✔ Permite análise de tempo de processamento
     *
     * nullable = false → sempre deve existir updatable = false → nunca pode ser alterado depois
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ===========
    // ⏱️ TIMESTAMP DE ATUALIZAÇÃO
    // ===========

    /**
     * 🕒 DATA DE ÚLTIMA ATUALIZAÇÃO
     *
     * 👉 Atualizado automaticamente a cada UPDATE
     *
     * ✔ Permite rastrear mudanças ✔ Ajuda a identificar atrasos ou problemas
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    // =============
    // 🔁 CONTROLE DE CONCORRÊNCIA (CRÍTICO)
    // =============

    /**
     * 🔒 VERSION (OPTIMISTIC LOCKING)
     *
     * 👉 Controla concorrência entre múltiplos consumidores
     *
     * --------------- PROBLEMA QUE RESOLVE:
     *
     * Dois consumers processam o mesmo pedido ao mesmo tempo:
     *
     * ❌ sem version → ambos sobrescrevem ✅ com version → apenas um consegue
     *
     * -----------
     *
     * 👉 Impacto:
     *
     * ✔ evita inconsistência ✔ protege o banco ✔ garante segurança financeira
     */
    @Version
    private Long version;

    // ===========
    // ⚙️ CALLBACKS AUTOMÁTICOS DO JPA
    // ===========

    /**
     * 📌 Executado antes de inserir no banco
     *
     * 👉 Define automaticamente createdAt
     *
     * ✔ evita depender de código externo
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    /**
     * 📌 Executado antes de atualizar no banco
     *
     * 👉 Define automaticamente updatedAt
     *
     * ✔ mantém histórico consistente
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
