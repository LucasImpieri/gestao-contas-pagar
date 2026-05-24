package com.impieri.gestaocontaspagar.service;

import com.impieri.gestaocontaspagar.domain.Conta;
import com.impieri.gestaocontaspagar.domain.Fornecedor;
import com.impieri.gestaocontaspagar.domain.Situacao;
import com.impieri.gestaocontaspagar.domain.factory.ContaFactory;
import com.impieri.gestaocontaspagar.dto.ContaRequest;
import com.impieri.gestaocontaspagar.dto.ContaResponse;
import com.impieri.gestaocontaspagar.repository.ContaRepository;
import com.impieri.gestaocontaspagar.repository.FornecedorRepository;
import com.impieri.gestaocontaspagar.web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

    @Mock
    private ContaRepository contaRepository;

    @Mock
    private FornecedorRepository fornecedorRepository;

    @Mock
    private ContaFactory contaFactory;

    @InjectMocks
    private ContaService contaService;

    @Test
    void deveCriarContaQuandoFornecedorExistir() {
        UUID fornecedorId = UUID.randomUUID();
        Fornecedor fornecedor = new Fornecedor("Claro");

        ContaRequest request = new ContaRequest(
                LocalDate.of(2026, 6, 10),
                BigDecimal.valueOf(150.75),
                "Conta de telefone",
                fornecedorId
        );

        Conta contaCriada = criarContaValida(fornecedor);
        Conta contaSalva = criarContaValida(fornecedor);

        when(fornecedorRepository.findById(fornecedorId))
                .thenReturn(Optional.of(fornecedor));

        when(contaFactory.criar(
                fornecedor,
                request.dataVencimento(),
                request.valor(),
                request.descricao()
        )).thenReturn(contaCriada);

        when(contaRepository.save(contaCriada))
                .thenReturn(contaSalva);

        ContaResponse response = contaService.criar(request);

        assertNotNull(response);
        assertEquals(contaSalva.getId(), response.id());
        assertEquals(contaSalva.getDataVencimento(), response.dataVencimento());
        assertEquals(contaSalva.getValor(), response.valor());
        assertEquals(contaSalva.getDescricao(), response.descricao());
        assertEquals(Situacao.PENDENTE, response.situacao());
        assertEquals("Claro", response.fornecedorNome());

        verify(fornecedorRepository).findById(fornecedorId);

        verify(contaFactory).criar(
                fornecedor,
                request.dataVencimento(),
                request.valor(),
                request.descricao()
        );

        verify(contaRepository).save(contaCriada);
    }

    @Test
    void naoDeveCriarContaQuandoFornecedorNaoExistir() {
        UUID fornecedorId = UUID.randomUUID();

        ContaRequest request = new ContaRequest(
                LocalDate.of(2026, 6, 10),
                BigDecimal.valueOf(150.75),
                "Conta de telefone",
                fornecedorId
        );

        when(fornecedorRepository.findById(fornecedorId))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> contaService.criar(request)
        );

        assertEquals("Fornecedor não encontrado: " + fornecedorId, exception.getMessage());

        verify(fornecedorRepository).findById(fornecedorId);

        verifyNoInteractions(contaFactory);

        verify(contaRepository, never()).save(any());
    }

    @Test
    void deveBuscarContasComFiltrosEPaginacao() {
        Fornecedor fornecedor = new Fornecedor("Claro");
        Conta conta = criarContaValida(fornecedor);

        PageRequest pageable = PageRequest.of(0, 10);

        when(contaRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(conta), pageable, 1));

        var resultado = contaService.buscar(
                "telefone",
                conta.getDataVencimento(),
                pageable
        );

        assertEquals(1, resultado.getTotalElements());

        assertEquals(
                conta.getId(),
                resultado.getContent().getFirst().id()
        );

        assertEquals(
                "Claro",
                resultado.getContent().getFirst().fornecedorNome()
        );

        verify(contaRepository)
                .findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void deveBuscarContaPorId() {
        UUID contaId = UUID.randomUUID();

        Fornecedor fornecedor = new Fornecedor("Claro");

        Conta conta = criarContaValida(fornecedor);

        conta.setId(contaId);

        when(contaRepository.findByIdWithFornecedor(contaId))
                .thenReturn(Optional.of(conta));

        ContaResponse response = contaService.buscarPorId(contaId);

        assertEquals(contaId, response.id());

        assertEquals("Claro", response.fornecedorNome());

        verify(contaRepository).findByIdWithFornecedor(contaId);
    }

    @Test
    void deveLancarErroQuandoContaNaoForEncontradaPorId() {
        UUID contaId = UUID.randomUUID();

        when(contaRepository.findByIdWithFornecedor(contaId))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> contaService.buscarPorId(contaId)
        );

        assertEquals("Conta não encontrada: " + contaId, exception.getMessage());

        verify(contaRepository).findByIdWithFornecedor(contaId);
    }

    @Test
    void deveAlterarSituacaoParaPago() {
        UUID contaId = UUID.randomUUID();

        Fornecedor fornecedor = new Fornecedor("Claro");

        Conta conta = criarContaValida(fornecedor);

        conta.setSituacao(Situacao.PENDENTE);

        when(contaRepository.findByIdWithFornecedor(contaId))
                .thenReturn(Optional.of(conta));

        ContaResponse response =
                contaService.alterarSituacao(contaId, Situacao.PAGO);

        assertEquals(Situacao.PAGO, response.situacao());

        assertNotNull(response.dataPagamento());

        verify(contaRepository).findByIdWithFornecedor(contaId);
    }

    @Test
    void deveAlterarSituacaoParaCancelado() {
        UUID contaId = UUID.randomUUID();

        Fornecedor fornecedor = new Fornecedor("Claro");

        Conta conta = criarContaValida(fornecedor);

        conta.setSituacao(Situacao.PENDENTE);

        when(contaRepository.findByIdWithFornecedor(contaId))
                .thenReturn(Optional.of(conta));

        ContaResponse response =
                contaService.alterarSituacao(contaId, Situacao.CANCELADO);

        assertEquals(Situacao.CANCELADO, response.situacao());

        assertNull(response.dataPagamento());

        verify(contaRepository).findByIdWithFornecedor(contaId);
    }

    @Test
    void naoDeveAlterarSituacaoQuandoNovaSituacaoForNula() {
        UUID contaId = UUID.randomUUID();

        Fornecedor fornecedor = new Fornecedor("Claro");

        Conta conta = criarContaValida(fornecedor);

        when(contaRepository.findByIdWithFornecedor(contaId))
                .thenReturn(Optional.of(conta));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> contaService.alterarSituacao(contaId, null)
        );

        assertEquals("Nova situação é obrigatória", exception.getMessage());

        verify(contaRepository).findByIdWithFornecedor(contaId);
    }

    @Test
    void deveCalcularTotalPagoPorPeriodo() {
        LocalDate dataInicio = LocalDate.of(2026, 5, 1);

        LocalDate dataFim = LocalDate.of(2026, 5, 31);

        when(contaRepository.totalPagoPorPeriodo(dataInicio, dataFim))
                .thenReturn(BigDecimal.valueOf(300.50));

        BigDecimal total =
                contaService.calcularTotalPagoPorPeriodo(dataInicio, dataFim);

        assertEquals(BigDecimal.valueOf(300.50), total);

        verify(contaRepository)
                .totalPagoPorPeriodo(dataInicio, dataFim);
    }

    @Test
    void naoDeveCalcularTotalPagoComPeriodoNulo() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> contaService.calcularTotalPagoPorPeriodo(
                        null,
                        LocalDate.now()
                )
        );

        assertEquals(
                "Data inicial e data final são obrigatórias",
                exception.getMessage()
        );

        verifyNoInteractions(contaRepository);
    }

    @Test
    void naoDeveCalcularTotalPagoQuandoDataInicioForDepoisDaDataFim() {
        LocalDate dataInicio = LocalDate.of(2026, 6, 1);

        LocalDate dataFim = LocalDate.of(2026, 5, 1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> contaService.calcularTotalPagoPorPeriodo(
                        dataInicio,
                        dataFim
                )
        );

        assertEquals(
                "Data inicial não pode ser posterior à data final",
                exception.getMessage()
        );

        verifyNoInteractions(contaRepository);
    }

    private Conta criarContaValida(Fornecedor fornecedor) {
        Conta conta = new Conta();

        conta.setId(UUID.randomUUID());

        conta.setFornecedor(fornecedor);

        conta.setDataVencimento(LocalDate.of(2026, 6, 10));

        conta.setValor(BigDecimal.valueOf(150.75));

        conta.setDescricao("Conta de telefone");

        conta.setSituacao(Situacao.PENDENTE);

        return conta;
    }
}