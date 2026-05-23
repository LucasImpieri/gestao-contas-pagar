package com.impieri.gestaocontaspagar.messaging;

import java.io.Serializable;

public record CsvImportMessage(
        String protocolo,
        String caminhoArquivo
) implements Serializable {
}