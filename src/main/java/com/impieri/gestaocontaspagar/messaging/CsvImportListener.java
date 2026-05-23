package com.impieri.gestaocontaspagar.messaging;

import com.impieri.gestaocontaspagar.domain.Fornecedor;
import com.impieri.gestaocontaspagar.dto.ContaRequest;
import com.impieri.gestaocontaspagar.service.ContaService;
import com.impieri.gestaocontaspagar.service.FornecedorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvImportListener {

    private static final Logger log = LoggerFactory.getLogger(CsvImportListener.class);

    private final FornecedorService fornecedorService;
    private final ContaService contaService;

    public CsvImportListener(
            FornecedorService fornecedorService,
            ContaService contaService
    ) {
        this.fornecedorService = fornecedorService;
        this.contaService = contaService;
    }

    @RabbitListener(queues = "csv.import")
    public void processar(CsvImportMessage mensagem) {
        log.info(
                "Iniciando importação CSV protocolo={} caminho={}",
                mensagem.protocolo(),
                mensagem.caminhoArquivo()
        );

        List<String> erros = new ArrayList<>();
        int linhaAtual = 0;
        int totalSucesso = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(mensagem.caminhoArquivo()))) {
            reader.readLine(); // ignora cabeçalho

            String linha;

            while ((linha = reader.readLine()) != null) {
                linhaAtual++;

                try {
                    processarLinha(linha);
                    totalSucesso++;

                } catch (Exception ex) {
                    String erro = "linha=" + linhaAtual + " erro=" + ex.getMessage();
                    log.error("Erro ao processar linha do CSV: {}", erro, ex);
                    erros.add(erro);
                }
            }

        } catch (Exception ex) {
            log.error(
                    "Falha geral ao processar CSV protocolo={} caminho={}",
                    mensagem.protocolo(),
                    mensagem.caminhoArquivo(),
                    ex
            );
            return;
        }

        log.info(
                "Importação CSV finalizada protocolo={} sucesso={} erros={}",
                mensagem.protocolo(),
                totalSucesso,
                erros.size()
        );
    }

    private void processarLinha(String linha) {
        String[] campos = linha.split(",", -1);

        validarQuantidadeCampos(campos);

        String fornecedorNome = campos[0].trim();
        String dataVencimentoTexto = campos[1].trim();
        String valorTexto = campos[2].trim();
        String descricao = campos[3].trim();

        Fornecedor fornecedor = fornecedorService.buscarOuCriarPorNome(fornecedorNome);

        ContaRequest request = new ContaRequest(
                LocalDate.parse(dataVencimentoTexto),
                new BigDecimal(valorTexto),
                descricao,
                fornecedor.getId()
        );

        contaService.criar(request);
    }

    private void validarQuantidadeCampos(String[] campos) {
        if (campos.length < 4) {
            throw new IllegalArgumentException(
                    "Linha inválida. Esperado: fornecedor_nome,data_vencimento,valor,descricao"
            );
        }
    }
}