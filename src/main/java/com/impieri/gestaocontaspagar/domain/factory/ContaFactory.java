package com.impieri.gestaocontaspagar.domain.factory;

import com.impieri.gestaocontaspagar.domain.Conta;
import com.impieri.gestaocontaspagar.domain.Fornecedor;
import com.impieri.gestaocontaspagar.domain.Situacao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class ContaFactory {

    public Conta criar(
            Fornecedor fornecedor,
            LocalDate dataVencimento,
            BigDecimal valor,
            String descricao
    ) {

        validarFornecedor(fornecedor);
        validarValor(valor);
        validarDataVencimento(dataVencimento);

        Conta conta = new Conta();

        conta.setId(UUID.randomUUID());
        conta.setFornecedor(fornecedor);
        conta.setDataVencimento(dataVencimento);
        conta.setValor(valor);
        conta.setDescricao(descricao);
        conta.setSituacao(Situacao.PENDENTE);

        return conta;
    }

    private void validarFornecedor(Fornecedor fornecedor) {
        if (fornecedor == null) {
            throw new IllegalArgumentException("Fornecedor é obrigatório");
        }
    }

    private void validarValor(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("Valor deve ser positivo");
        }
    }

    private void validarDataVencimento(LocalDate dataVencimento) {
        if (dataVencimento == null) {
            throw new IllegalArgumentException("Data de vencimento é obrigatória");
        }
    }
}