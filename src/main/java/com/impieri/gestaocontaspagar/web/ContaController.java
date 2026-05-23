package com.impieri.gestaocontaspagar.web;

import com.impieri.gestaocontaspagar.domain.Situacao;
import com.impieri.gestaocontaspagar.dto.ContaRequest;
import com.impieri.gestaocontaspagar.dto.ContaResponse;
import com.impieri.gestaocontaspagar.messaging.CsvImportProducer;
import com.impieri.gestaocontaspagar.service.ContaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/contas")
public class ContaController {

    private final ContaService contaService;
    private final CsvImportProducer producer;

    public ContaController(
            ContaService contaService,
            CsvImportProducer producer
    ) {
        this.contaService = contaService;
        this.producer = producer;
    }

    @PostMapping
    public ResponseEntity<ContaResponse> criar(@Valid @RequestBody ContaRequest request) {
        ContaResponse response = contaService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ContaResponse>> listar(
            @RequestParam(required = false) String descricao,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataVencimento,

            @PageableDefault(size = 20)
            Pageable pageable
    ) {
        Page<ContaResponse> response = contaService.buscar(descricao, dataVencimento, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContaResponse> buscarPorId(@PathVariable UUID id) {
        ContaResponse response = contaService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/situacao")
    public ResponseEntity<ContaResponse> alterarSituacao(
            @PathVariable UUID id,
            @RequestParam Situacao situacao
    ) {
        ContaResponse response = contaService.alterarSituacao(id, situacao);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/relatorio/total-pago")
    public ResponseEntity<Map<String, Object>> calcularTotalPagoPorPeriodo(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataInicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataFim
    ) {
        BigDecimal total = contaService.calcularTotalPagoPorPeriodo(dataInicio, dataFim);

        return ResponseEntity.ok(Map.of(
                "dataInicio", dataInicio,
                "dataFim", dataFim,
                "total", total
        ));
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, String>> importarCsv(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String protocolo = UUID.randomUUID().toString();

        File arquivoTemporario = Files
                .createTempFile("import-" + protocolo + "-", ".csv")
                .toFile();

        file.transferTo(arquivoTemporario);

        producer.publicarImportacao(protocolo, arquivoTemporario.getAbsolutePath());

        return ResponseEntity.accepted().body(Map.of(
                "protocolo", protocolo
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContaResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ContaRequest request
    ) {
        ContaResponse response = contaService.atualizar(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        contaService.deletar(id);
        return ResponseEntity.noContent().build();
    }

}