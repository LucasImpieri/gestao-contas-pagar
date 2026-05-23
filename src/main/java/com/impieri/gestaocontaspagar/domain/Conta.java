package com.impieri.gestaocontaspagar.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "conta")
public class Conta {

    @Id
    private UUID id;

    @NotNull
    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(columnDefinition = "text")
    private String descricao;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Situacao situacao;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @PrePersist
    private void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }

        if (this.situacao == null) {
            this.situacao = Situacao.PENDENTE;
        }
    }

    public void marcarComoPaga(LocalDate dataPagamento) {
        if (this.situacao == Situacao.CANCELADO) {
            throw new IllegalStateException("Conta cancelada não pode ser paga");
        }

        if (this.situacao == Situacao.PAGO) {
            return;
        }

        this.situacao = Situacao.PAGO;
        this.dataPagamento = dataPagamento != null ? dataPagamento : LocalDate.now();
    }

    public void cancelar() {
        if (this.situacao == Situacao.PAGO) {
            throw new IllegalStateException("Conta paga não pode ser cancelada");
        }

        if (this.situacao == Situacao.CANCELADO) {
            return;
        }

        this.situacao = Situacao.CANCELADO;
        this.dataPagamento = null;
    }

    public void voltarParaPendente() {
        if (this.situacao == Situacao.PAGO) {
            throw new IllegalStateException("Conta paga não pode voltar para pendente");
        }

        this.situacao = Situacao.PENDENTE;
        this.dataPagamento = null;
    }

    public void alterarValor(BigDecimal novoValor) {
        if (novoValor == null || novoValor.signum() <= 0) {
            throw new IllegalArgumentException("Valor da conta deve ser positivo");
        }

        this.valor = novoValor;
    }

    public boolean estaPaga() {
        return this.situacao == Situacao.PAGO;
    }

    public boolean estaCancelada() {
        return this.situacao == Situacao.CANCELADO;
    }

    public void atualizarDados(
            LocalDate dataVencimento,
            BigDecimal valor,
            String descricao,
            Fornecedor fornecedor
    ) {
        if (this.situacao == Situacao.PAGO) {
            throw new IllegalStateException("Conta paga não pode ser alterada");
        }

        if (this.situacao == Situacao.CANCELADO) {
            throw new IllegalStateException("Conta cancelada não pode ser alterada");
        }

        if (dataVencimento == null) {
            throw new IllegalArgumentException("Data de vencimento é obrigatória");
        }

        if (fornecedor == null) {
            throw new IllegalArgumentException("Fornecedor é obrigatório");
        }

        alterarValor(valor);

        this.dataVencimento = dataVencimento;
        this.descricao = descricao;
        this.fornecedor = fornecedor;
    }
}