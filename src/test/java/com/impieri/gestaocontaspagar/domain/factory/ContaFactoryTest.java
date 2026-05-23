package com.impieri.gestaocontaspagar.domain.factory;

import com.impieri.gestaocontaspagar.domain.Conta;
import com.impieri.gestaocontaspagar.domain.Fornecedor;
import com.impieri.gestaocontaspagar.domain.Situacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ContaFactoryTest {

    private final ContaFactory contaFactory = new ContaFactory();

    @Test
    void deveCriarContaPendenteComDadosValidos() {
        Fornecedor fornecedor = new Fornecedor("Claro");
        LocalDate dataVencimento = LocalDate.of(2026, 6, 10);
        BigDecimal valor = BigDecimal.valueOf(150.75);
        String descricao = "Conta de telefone";

        Conta conta = contaFactory.criar(
                fornecedor,
                dataVencimento,
                valor,
                descricao
        );

        assertNotNull(conta.getId());
        assertEquals(fornecedor, conta.getFornecedor());
        assertEquals(dataVencimento, conta.getDataVencimento());
        assertEquals(valor, conta.getValor());
        assertEquals(descricao, conta.getDescricao());
        assertEquals(Situacao.PENDENTE, conta.getSituacao());
        assertNull(conta.getDataPagamento());
    }

    @Test
    void naoDeveCriarContaSemFornecedor() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> contaFactory.criar(
                        null,
                        LocalDate.of(2026, 6, 10),
                        BigDecimal.TEN,
                        "Conta sem fornecedor"
                )
        );

        assertEquals("Fornecedor é obrigatório", exception.getMessage());
    }

    @Test
    void naoDeveCriarContaSemDataDeVencimento() {
        Fornecedor fornecedor = new Fornecedor("Claro");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> contaFactory.criar(
                        fornecedor,
                        null,
                        BigDecimal.TEN,
                        "Conta sem vencimento"
                )
        );

        assertEquals("Data de vencimento é obrigatória", exception.getMessage());
    }

    @Test
    void naoDeveCriarContaComValorNulo() {
        Fornecedor fornecedor = new Fornecedor("Claro");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> contaFactory.criar(
                        fornecedor,
                        LocalDate.of(2026, 6, 10),
                        null,
                        "Conta sem valor"
                )
        );

        assertEquals("Valor deve ser positivo", exception.getMessage());
    }

    @Test
    void naoDeveCriarContaComValorZeroOuNegativo() {
        Fornecedor fornecedor = new Fornecedor("Claro");
        LocalDate dataVencimento = LocalDate.of(2026, 6, 10);

        assertThrows(
                IllegalArgumentException.class,
                () -> contaFactory.criar(
                        fornecedor,
                        dataVencimento,
                        BigDecimal.ZERO,
                        "Conta com valor zero"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> contaFactory.criar(
                        fornecedor,
                        dataVencimento,
                        BigDecimal.valueOf(-10),
                        "Conta com valor negativo"
                )
        );
    }
}