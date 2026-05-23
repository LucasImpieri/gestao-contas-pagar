package com.impieri.gestaocontaspagar.dto;

import com.impieri.gestaocontaspagar.domain.Situacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContaResponse(
        UUID id,
        LocalDate dataVencimento,
        LocalDate dataPagamento,
        BigDecimal valor,
        String descricao,
        Situacao situacao,
        UUID fornecedorId,
        String fornecedorNome
) {
}