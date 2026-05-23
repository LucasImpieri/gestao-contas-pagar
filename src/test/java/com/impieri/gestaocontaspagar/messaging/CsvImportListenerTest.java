package com.impieri.gestaocontaspagar.messaging;

import com.impieri.gestaocontaspagar.domain.Fornecedor;
import com.impieri.gestaocontaspagar.dto.ContaRequest;
import com.impieri.gestaocontaspagar.service.ContaService;
import com.impieri.gestaocontaspagar.service.FornecedorService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CsvImportListenerTest {

    @Test
    void deveProcessarCsvComLinhasValidas() throws Exception {
        FornecedorService fornecedorService = mock(FornecedorService.class);
        ContaService contaService = mock(ContaService.class);

        CsvImportListener listener = new CsvImportListener(fornecedorService, contaService);

        UUID fornecedorId = UUID.randomUUID();

        Fornecedor fornecedor = new Fornecedor("Claro");
        ReflectionTestUtils.setField(fornecedor, "id", fornecedorId);

        when(fornecedorService.buscarOuCriarPorNome("Claro")).thenReturn(fornecedor);
        when(fornecedorService.buscarOuCriarPorNome("TIM")).thenReturn(fornecedor);

        Path arquivo = Files.createTempFile("contas-validas", ".csv");

        Files.writeString(
                arquivo,
                """
                fornecedor_nome,data_vencimento,valor,descricao
                Claro,2026-06-10,150.75,Conta de telefone
                TIM,2026-06-15,89.90,Conta de internet
                """
        );

        CsvImportMessage mensagem = new CsvImportMessage(
                "protocolo-123",
                arquivo.toString()
        );

        listener.processar(mensagem);

        ArgumentCaptor<ContaRequest> captor = ArgumentCaptor.forClass(ContaRequest.class);

        verify(contaService, times(2)).criar(captor.capture());

        List<ContaRequest> requests = captor.getAllValues();

        assertEquals(LocalDate.of(2026, 6, 10), requests.get(0).dataVencimento());
        assertEquals(0, new BigDecimal("150.75").compareTo(requests.get(0).valor()));
        assertEquals("Conta de telefone", requests.get(0).descricao());
        assertEquals(fornecedorId, requests.get(0).fornecedorId());

        assertEquals(LocalDate.of(2026, 6, 15), requests.get(1).dataVencimento());
        assertEquals(0, new BigDecimal("89.90").compareTo(requests.get(1).valor()));
        assertEquals("Conta de internet", requests.get(1).descricao());
        assertEquals(fornecedorId, requests.get(1).fornecedorId());

        verify(fornecedorService).buscarOuCriarPorNome("Claro");
        verify(fornecedorService).buscarOuCriarPorNome("TIM");
    }

    @Test
    void deveContinuarProcessamentoQuandoUmaLinhaFalhar() throws Exception {
        FornecedorService fornecedorService = mock(FornecedorService.class);
        ContaService contaService = mock(ContaService.class);

        CsvImportListener listener = new CsvImportListener(fornecedorService, contaService);

        UUID fornecedorId = UUID.randomUUID();

        Fornecedor fornecedor = new Fornecedor("Claro");
        ReflectionTestUtils.setField(fornecedor, "id", fornecedorId);

        when(fornecedorService.buscarOuCriarPorNome("Claro")).thenReturn(fornecedor);

        Path arquivo = Files.createTempFile("contas-com-erro", ".csv");

        Files.writeString(
                arquivo,
                """
                fornecedor_nome,data_vencimento,valor,descricao
                linha_invalida
                Claro,2026-06-10,150.75,Conta de telefone
                """
        );

        CsvImportMessage mensagem = new CsvImportMessage(
                "protocolo-456",
                arquivo.toString()
        );

        listener.processar(mensagem);

        verify(contaService, times(1)).criar(any(ContaRequest.class));
        verify(fornecedorService, times(1)).buscarOuCriarPorNome("Claro");
    }

    @Test
    void naoDeveProcessarQuandoArquivoNaoExistir() {
        FornecedorService fornecedorService = mock(FornecedorService.class);
        ContaService contaService = mock(ContaService.class);

        CsvImportListener listener = new CsvImportListener(fornecedorService, contaService);

        CsvImportMessage mensagem = new CsvImportMessage(
                "protocolo-789",
                "/arquivo/inexistente.csv"
        );

        listener.processar(mensagem);

        verifyNoInteractions(fornecedorService);
        verifyNoInteractions(contaService);
    }
}