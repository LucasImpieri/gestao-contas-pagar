package com.impieri.gestaocontaspagar.service;

import com.impieri.gestaocontaspagar.domain.Fornecedor;
import com.impieri.gestaocontaspagar.repository.FornecedorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FornecedorServiceTest {

    @Mock
    private FornecedorRepository fornecedorRepository;

    @InjectMocks
    private FornecedorService fornecedorService;

    @Test
    void deveRetornarFornecedorExistenteQuandoNomeJaEstiverCadastrado() {
        Fornecedor fornecedor = new Fornecedor("Claro");

        when(fornecedorRepository.findByNomeIgnoreCase("Claro"))
                .thenReturn(Optional.of(fornecedor));

        Fornecedor resultado = fornecedorService.buscarOuCriarPorNome("Claro");

        assertSame(fornecedor, resultado);

        verify(fornecedorRepository).findByNomeIgnoreCase("Claro");
        verify(fornecedorRepository, never()).save(any());
    }

    @Test
    void deveCriarFornecedorQuandoNomeNaoEstiverCadastrado() {
        Fornecedor fornecedorSalvo = new Fornecedor("Claro");

        when(fornecedorRepository.findByNomeIgnoreCase("Claro"))
                .thenReturn(Optional.empty());

        when(fornecedorRepository.save(any(Fornecedor.class)))
                .thenReturn(fornecedorSalvo);

        Fornecedor resultado = fornecedorService.buscarOuCriarPorNome("  Claro  ");

        assertSame(fornecedorSalvo, resultado);

        verify(fornecedorRepository).findByNomeIgnoreCase("Claro");
        verify(fornecedorRepository).save(any(Fornecedor.class));
    }

    @Test
    void naoDeveBuscarOuCriarFornecedorComNomeNulo() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fornecedorService.buscarOuCriarPorNome(null)
        );

        assertEquals("Nome do fornecedor é obrigatório", exception.getMessage());

        verifyNoInteractions(fornecedorRepository);
    }

    @Test
    void naoDeveBuscarOuCriarFornecedorComNomeEmBranco() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fornecedorService.buscarOuCriarPorNome("   ")
        );

        assertEquals("Nome do fornecedor é obrigatório", exception.getMessage());

        verifyNoInteractions(fornecedorRepository);
    }
}