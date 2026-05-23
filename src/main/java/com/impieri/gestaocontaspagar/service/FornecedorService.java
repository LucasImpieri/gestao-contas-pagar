package com.impieri.gestaocontaspagar.service;

import com.impieri.gestaocontaspagar.domain.Fornecedor;
import com.impieri.gestaocontaspagar.repository.FornecedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;

    public FornecedorService(FornecedorRepository fornecedorRepository) {
        this.fornecedorRepository = fornecedorRepository;
    }

    @Transactional
    public Fornecedor buscarOuCriarPorNome(String nome) {
        validarNome(nome);

        String nomeNormalizado = nome.trim();

        return fornecedorRepository.findByNomeIgnoreCase(nomeNormalizado)
                .orElseGet(() -> fornecedorRepository.save(new Fornecedor(nomeNormalizado)));
    }

    private void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do fornecedor é obrigatório");
        }
    }
}