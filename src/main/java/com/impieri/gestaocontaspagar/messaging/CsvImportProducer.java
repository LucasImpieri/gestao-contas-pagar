package com.impieri.gestaocontaspagar.messaging;

import com.impieri.gestaocontaspagar.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class CsvImportProducer {

    private final RabbitTemplate rabbitTemplate;

    public CsvImportProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarImportacao(String protocolo, String caminhoArquivo) {
        CsvImportMessage mensagem = new CsvImportMessage(protocolo, caminhoArquivo);
        rabbitTemplate.convertAndSend(RabbitConfig.CSV_IMPORT_QUEUE, mensagem);
    }
}