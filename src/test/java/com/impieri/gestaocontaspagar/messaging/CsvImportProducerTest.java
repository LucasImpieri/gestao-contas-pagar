package com.impieri.gestaocontaspagar.messaging;

import com.impieri.gestaocontaspagar.config.RabbitConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CsvImportProducerTest {

    @Test
    void devePublicarMensagemDeImportacaoNaFilaCorreta() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        CsvImportProducer producer = new CsvImportProducer(rabbitTemplate);

        String protocolo = "protocolo-123";
        String caminhoArquivo = "/tmp/arquivo.csv";

        producer.publicarImportacao(protocolo, caminhoArquivo);

        ArgumentCaptor<CsvImportMessage> captor = ArgumentCaptor.forClass(CsvImportMessage.class);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.CSV_IMPORT_QUEUE),
                captor.capture()
        );

        CsvImportMessage mensagem = captor.getValue();

        assertEquals(protocolo, mensagem.protocolo());
        assertEquals(caminhoArquivo, mensagem.caminhoArquivo());
    }
}