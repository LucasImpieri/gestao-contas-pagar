package com.impieri.gestaocontaspagar.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.impieri.gestaocontaspagar.domain.Situacao;
import com.impieri.gestaocontaspagar.dto.ContaRequest;
import com.impieri.gestaocontaspagar.dto.ContaResponse;
import com.impieri.gestaocontaspagar.messaging.CsvImportProducer;
import com.impieri.gestaocontaspagar.service.ContaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ContaControllerTest {

    private ContaService contaService;
    private CsvImportProducer producer;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        contaService = mock(ContaService.class);
        producer = mock(CsvImportProducer.class);

        ContaController controller = new ContaController(contaService, producer);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void deveCriarConta() throws Exception {
        UUID fornecedorId = UUID.randomUUID();
        UUID contaId = UUID.randomUUID();

        ContaRequest request = new ContaRequest(
                LocalDate.of(2026, 6, 10),
                BigDecimal.valueOf(150.75),
                "Conta de telefone",
                fornecedorId
        );

        ContaResponse response = new ContaResponse(
                contaId,
                request.dataVencimento(),
                null,
                request.valor(),
                request.descricao(),
                Situacao.PENDENTE,
                fornecedorId,
                "Claro"
        );

        when(contaService.criar(any(ContaRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/contas")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(contaId.toString()))
                .andExpect(jsonPath("$.situacao").value("PENDENTE"))
                .andExpect(jsonPath("$.fornecedorNome").value("Claro"));

        ArgumentCaptor<ContaRequest> captor = ArgumentCaptor.forClass(ContaRequest.class);

        verify(contaService).criar(captor.capture());

        ContaRequest requestCapturado = captor.getValue();

        org.junit.jupiter.api.Assertions.assertEquals(request.dataVencimento(), requestCapturado.dataVencimento());
        org.junit.jupiter.api.Assertions.assertEquals(request.valor(), requestCapturado.valor());
        org.junit.jupiter.api.Assertions.assertEquals(request.descricao(), requestCapturado.descricao());
        org.junit.jupiter.api.Assertions.assertEquals(request.fornecedorId(), requestCapturado.fornecedorId());
    }

    @Test
    void deveListarContasComFiltrosEPaginacao() throws Exception {
        UUID fornecedorId = UUID.randomUUID();
        UUID contaId = UUID.randomUUID();

        ContaResponse contaResponse = new ContaResponse(
                contaId,
                LocalDate.of(2026, 6, 10),
                null,
                BigDecimal.valueOf(150.75),
                "Conta de telefone",
                Situacao.PENDENTE,
                fornecedorId,
                "Claro"
        );

        when(contaService.buscar(
                eq("telefone"),
                eq(LocalDate.of(2026, 6, 10)),
                any()
        )).thenReturn(new PageImpl<>(
                List.of(contaResponse),
                PageRequest.of(0, 20),
                1
        ));

        mockMvc.perform(get("/api/contas")
                        .param("descricao", "telefone")
                        .param("dataVencimento", "2026-06-10")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(contaId.toString()))
                .andExpect(jsonPath("$.content[0].fornecedorNome").value("Claro"));

        verify(contaService).buscar(
                eq("telefone"),
                eq(LocalDate.of(2026, 6, 10)),
                any()
        );
    }

    @Test
    void deveBuscarContaPorId() throws Exception {
        UUID contaId = UUID.randomUUID();
        UUID fornecedorId = UUID.randomUUID();

        ContaResponse response = new ContaResponse(
                contaId,
                LocalDate.of(2026, 6, 10),
                null,
                BigDecimal.valueOf(150.75),
                "Conta de telefone",
                Situacao.PENDENTE,
                fornecedorId,
                "Claro"
        );

        when(contaService.buscarPorId(contaId)).thenReturn(response);

        mockMvc.perform(get("/api/contas/{id}", contaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contaId.toString()))
                .andExpect(jsonPath("$.fornecedorNome").value("Claro"));

        verify(contaService).buscarPorId(contaId);
    }

    @Test
    void deveAlterarSituacao() throws Exception {
        UUID contaId = UUID.randomUUID();
        UUID fornecedorId = UUID.randomUUID();

        ContaResponse response = new ContaResponse(
                contaId,
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 11),
                BigDecimal.valueOf(150.75),
                "Conta de telefone",
                Situacao.PAGO,
                fornecedorId,
                "Claro"
        );

        when(contaService.alterarSituacao(contaId, Situacao.PAGO)).thenReturn(response);

        mockMvc.perform(patch("/api/contas/{id}/situacao", contaId)
                        .param("situacao", "PAGO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("PAGO"))
                .andExpect(jsonPath("$.dataPagamento").value("2026-06-11"));

        verify(contaService).alterarSituacao(contaId, Situacao.PAGO);
    }

    @Test
    void deveCalcularTotalPagoPorPeriodo() throws Exception {
        when(contaService.calcularTotalPagoPorPeriodo(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        )).thenReturn(BigDecimal.valueOf(300.50));

        mockMvc.perform(get("/api/contas/relatorio/total-pago")
                        .param("dataInicio", "2026-05-01")
                        .param("dataFim", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataInicio").value("2026-05-01"))
                .andExpect(jsonPath("$.dataFim").value("2026-05-31"))
                .andExpect(jsonPath("$.total").value(300.5));

        verify(contaService).calcularTotalPagoPorPeriodo(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );
    }

    @Test
    void deveImportarCsvERetornarProtocolo() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "file",
                "contas.csv",
                "text/csv",
                """
                fornecedor_nome,data_vencimento,valor,descricao
                Claro,2026-06-10,150.75,Conta de telefone
                """.getBytes()
        );

        mockMvc.perform(multipart("/api/contas/import").file(arquivo))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.protocolo", notNullValue()));

        verify(producer).publicarImportacao(anyString(), anyString());
    }
}