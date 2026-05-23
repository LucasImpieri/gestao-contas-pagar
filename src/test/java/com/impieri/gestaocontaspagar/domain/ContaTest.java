package com.impieri.gestaocontaspagar.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ContaTest {

    @Test
    void deveMarcarContaPendenteComoPaga() {
        Conta conta = new Conta();
        conta.setSituacao(Situacao.PENDENTE);

        LocalDate dataPagamento = LocalDate.of(2026, 6, 10);

        conta.marcarComoPaga(dataPagamento);

        assertEquals(Situacao.PAGO, conta.getSituacao());
        assertEquals(dataPagamento, conta.getDataPagamento());
        assertTrue(conta.estaPaga());
    }

    @Test
    void deveUsarDataAtualQuandoDataPagamentoForNula() {
        Conta conta = new Conta();
        conta.setSituacao(Situacao.PENDENTE);

        conta.marcarComoPaga(null);

        assertEquals(Situacao.PAGO, conta.getSituacao());
        assertEquals(LocalDate.now(), conta.getDataPagamento());
    }

    @Test
    void naoDevePagarContaCancelada() {
        Conta conta = new Conta();
        conta.setSituacao(Situacao.CANCELADO);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> conta.marcarComoPaga(LocalDate.now())
        );

        assertEquals("Conta cancelada não pode ser paga", exception.getMessage());
    }

    @Test
    void deveManterContaPagaQuandoMarcarComoPagaNovamente() {
        Conta conta = new Conta();
        conta.setSituacao(Situacao.PAGO);

        LocalDate dataPagamentoOriginal = LocalDate.of(2026, 5, 1);
        conta.setDataPagamento(dataPagamentoOriginal);

        conta.marcarComoPaga(LocalDate.of(2026, 6, 1));

        assertEquals(Situacao.PAGO, conta.getSituacao());
        assertEquals(dataPagamentoOriginal, conta.getDataPagamento());
    }

    @Test
    void naoDeveCancelarContaPaga() {
        Conta conta = new Conta();
        conta.setSituacao(Situacao.PAGO);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                conta::cancelar
        );

        assertEquals("Conta paga não pode ser cancelada", exception.getMessage());
    }

    @Test
    void deveCancelarContaPendente() {
        Conta conta = new Conta();
        conta.setSituacao(Situacao.PENDENTE);
        conta.setDataPagamento(LocalDate.of(2026, 6, 10));

        conta.cancelar();

        assertEquals(Situacao.CANCELADO, conta.getSituacao());
        assertNull(conta.getDataPagamento());
        assertTrue(conta.estaCancelada());
    }

    @Test
    void naoDeveVoltarContaPagaParaPendente() {
        Conta conta = new Conta();
        conta.setSituacao(Situacao.PAGO);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                conta::voltarParaPendente
        );

        assertEquals("Conta paga não pode voltar para pendente", exception.getMessage());
    }

    @Test
    void deveVoltarContaCanceladaParaPendente() {
        Conta conta = new Conta();
        conta.setSituacao(Situacao.CANCELADO);
        conta.setDataPagamento(LocalDate.of(2026, 6, 10));

        conta.voltarParaPendente();

        assertEquals(Situacao.PENDENTE, conta.getSituacao());
        assertNull(conta.getDataPagamento());
    }

    @Test
    void deveAlterarValorQuandoValorForPositivo() {
        Conta conta = new Conta();

        conta.alterarValor(BigDecimal.valueOf(100.50));

        assertEquals(BigDecimal.valueOf(100.50), conta.getValor());
    }

    @Test
    void naoDeveAlterarValorParaNulo() {
        Conta conta = new Conta();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> conta.alterarValor(null)
        );

        assertEquals("Valor da conta deve ser positivo", exception.getMessage());
    }

    @Test
    void naoDeveAlterarValorParaZeroOuNegativo() {
        Conta conta = new Conta();

        assertThrows(
                IllegalArgumentException.class,
                () -> conta.alterarValor(BigDecimal.ZERO)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> conta.alterarValor(BigDecimal.valueOf(-10))
        );
    }
}