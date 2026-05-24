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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class ContaService {

    private final ContaRepository contaRepository;
    private final FornecedorRepository fornecedorRepository;
    private final ContaFactory contaFactory;

    public ContaService(
            ContaRepository contaRepository,
            FornecedorRepository fornecedorRepository,
            ContaFactory contaFactory
    ) {
        this.contaRepository = contaRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.contaFactory = contaFactory;
    }

    @Transactional
    public ContaResponse criar(ContaRequest request) {
        Fornecedor fornecedor = fornecedorRepository.findById(request.fornecedorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fornecedor não encontrado: " + request.fornecedorId()
                ));

        Conta conta = contaFactory.criar(
                fornecedor,
                request.dataVencimento(),
                request.valor(),
                request.descricao()
        );

        Conta contaSalva = contaRepository.save(conta);

        return toResponse(contaSalva);
    }

    @Transactional(readOnly = true)
    public Page<ContaResponse> buscar(
            String descricao,
            LocalDate dataVencimento,
            Pageable pageable
    ) {
        Specification<Conta> spec = (root, query, cb) -> cb.conjunction();
        if (descricao != null && !descricao.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(
                            cb.lower(root.get("descricao")),
                            "%" + descricao.toLowerCase() + "%"
                    )
            );
        }
        if (dataVencimento != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("dataVencimento"), dataVencimento)
            );
        }
        return contaRepository.findAll(spec, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ContaResponse buscarPorId(UUID id) {
        Conta conta = contaRepository.findByIdWithFornecedor(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada: " + id));

        return toResponse(conta);
    }

    @Transactional
    public ContaResponse alterarSituacao(UUID id, Situacao novaSituacao) {
        Conta conta = contaRepository.findByIdWithFornecedor(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada: " + id));

        if (novaSituacao == null) {
            throw new IllegalArgumentException("Nova situação é obrigatória");
        }

        switch (novaSituacao) {
            case PAGO -> conta.marcarComoPaga(LocalDate.now());
            case CANCELADO -> conta.cancelar();
            case PENDENTE -> conta.voltarParaPendente();
            default -> throw new IllegalArgumentException("Situação inválida: " + novaSituacao);
        }

        return toResponse(conta);
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularTotalPagoPorPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        validarPeriodo(dataInicio, dataFim);

        return contaRepository.totalPagoPorPeriodo(dataInicio, dataFim);
    }

    @Transactional
    public ContaResponse atualizar(UUID id, ContaRequest request) {
        Conta conta = contaRepository.findByIdWithFornecedor(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada: " + id));

        Fornecedor fornecedor = fornecedorRepository.findById(request.fornecedorId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Fornecedor não encontrado: " + request.fornecedorId()
                ));

        conta.atualizarDados(
                request.dataVencimento(),
                request.valor(),
                request.descricao(),
                fornecedor
        );

        return toResponse(conta);
    }

    @Transactional
    public void deletar(UUID id) {
        Conta conta = contaRepository.findByIdWithFornecedor(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada: " + id));

        if (conta.estaPaga()) {
            throw new IllegalStateException("Conta paga não pode ser excluída");
        }

        contaRepository.delete(conta);
    }

    private void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null || dataFim == null) {
            throw new IllegalArgumentException("Data inicial e data final são obrigatórias");
        }

        if (dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("Data inicial não pode ser posterior à data final");
        }
    }

    private ContaResponse toResponse(Conta conta) {
        Fornecedor fornecedor = conta.getFornecedor();

        return new ContaResponse(
                conta.getId(),
                conta.getDataVencimento(),
                conta.getDataPagamento(),
                conta.getValor(),
                conta.getDescricao(),
                conta.getSituacao(),
                fornecedor != null ? fornecedor.getId() : null,
                fornecedor != null ? fornecedor.getNome() : null
        );
    }
}