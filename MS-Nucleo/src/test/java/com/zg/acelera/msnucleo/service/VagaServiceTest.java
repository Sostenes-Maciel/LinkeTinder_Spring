package com.zg.acelera.msnucleo.service;

import com.zg.acelera.msnucleo.model.Empresa;
import com.zg.acelera.msnucleo.model.Vaga;
import com.zg.acelera.msnucleo.repository.EmpresaRepository;
import com.zg.acelera.msnucleo.repository.VagaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VagaServiceTest {

    private static final String CNPJ = "12345678000199";

    @Mock
    private VagaRepository vagaRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @InjectMocks
    private VagaService vagaService;

    private Empresa criarEmpresa() {
        return new Empresa(
                CNPJ, "Tech Global", "contato@techglobal.com",
                "Brasil", "PE", null, null
        );
    }

    private Vaga criarVaga(Empresa empresa) {
        return new Vaga(
                "Desenvolvedor Java",
                "Brasil",
                "PE",
                "Desenvolvimento backend",
                empresa
        );
    }

    @Test
    void deveCadastrarVagaParaEmpresaExistente() {
        Empresa empresa = criarEmpresa();

        when(empresaRepository.findById(CNPJ))
                .thenReturn(Optional.of(empresa));

        when(vagaRepository.save(any(Vaga.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Vaga resultado = vagaService.cadastrar(
                "Desenvolvedor Java",
                "Brasil",
                "PE",
                "Desenvolvimento backend",
                CNPJ
        );

        assertEquals("Desenvolvedor Java", resultado.getTitulo());
        assertEquals(CNPJ, resultado.getCnpjEmpresa());

        verify(vagaRepository).save(any(Vaga.class));
    }

    @Test
    void naoDeveCadastrarVagaParaEmpresaInexistente() {
        when(empresaRepository.findById(CNPJ))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> vagaService.cadastrar(
                        "Desenvolvedor Java",
                        null, null, null, CNPJ
                )
        );

        verify(vagaRepository, never()).save(any());
    }

    @Test
    void deveAtualizarDadosSemAlterarEmpresaProprietaria() {
        Empresa empresa = criarEmpresa();
        Vaga vaga = criarVaga(empresa);

        when(vagaRepository.findByIdVagaAndCnpjEmpresa(1L, CNPJ))
                .thenReturn(Optional.of(vaga));

        when(vagaRepository.save(any(Vaga.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Vaga resultado = vagaService.atualizar(
                1L,
                "Desenvolvedor Kotlin",
                "Brasil",
                "SP",
                "Nova descrição",
                CNPJ
        );

        assertEquals("Desenvolvedor Kotlin", resultado.getTitulo());
        assertEquals(CNPJ, resultado.getCnpjEmpresa());
    }

    @Test
    void naoDeveEncontrarVagaDeOutraEmpresa() {
        when(vagaRepository.findByIdVagaAndCnpjEmpresa(1L, CNPJ))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> vagaService.buscarPorIdEEmpresa(1L, CNPJ)
        );
    }

    @Test
    void deveExcluirVagaDaEmpresaInformada() {
        Empresa empresa = criarEmpresa();
        Vaga vaga = criarVaga(empresa);

        when(vagaRepository.findByIdVagaAndCnpjEmpresa(1L, CNPJ))
                .thenReturn(Optional.of(vaga));

        assertDoesNotThrow(() -> vagaService.excluir(1L, CNPJ));

        verify(vagaRepository).delete(vaga);
    }
}