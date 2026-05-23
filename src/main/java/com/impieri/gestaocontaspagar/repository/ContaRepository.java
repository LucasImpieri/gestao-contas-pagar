package com.impieri.gestaocontaspagar.repository;

import com.impieri.gestaocontaspagar.domain.Conta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContaRepository extends
        JpaRepository<Conta, UUID>,
        JpaSpecificationExecutor<Conta> {

    @EntityGraph(attributePaths = "fornecedor")
    @Query("""
        select c
        from Conta c
        where c.id = :id
        """)
    Optional<Conta> findByIdWithFornecedor(@Param("id") UUID id);

    @Query("""
        select coalesce(sum(c.valor), 0)
        from Conta c
        where c.situacao = com.impieri.gestaocontaspagar.domain.Situacao.PAGO
          and c.dataPagamento between :dataInicio and :dataFim
        """)
    BigDecimal totalPagoPorPeriodo(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim
    );
}