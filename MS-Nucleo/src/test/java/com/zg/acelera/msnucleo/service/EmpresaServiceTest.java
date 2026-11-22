package com.zg.acelera.msnucleo.service;

import com.zg.acelera.msnucleo.model.Empresa;
import com.zg.acelera.msnucleo.repository.EmpresaRepository;
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
class EmpresaServiceTest {

    private static final String CNPJ = "12345678000199";

    @Mock
    private EmpresaRepository empresaRepository;

    @InjectMocks
    private EmpresaService empresaService;

    private Empresa criarEmpresa() {
        return new Empresa(
                CNPJ, "Tech Global", "contato@techglobal.com",
                "Brasil", "PE", "50000-000", "Tecnologia"
        );
    }

    @Test
    void deveBuscarEmpresaNormalizandoCnpj() {
        Empresa empresa = criarEmpresa();

        when(empresaRepository.findById(CNPJ))
                .thenReturn(Optional.of(empresa));

        Empresa resultado =
                empresaService.buscarPorCnpj("12.345.678/0001-99");

        assertEquals(CNPJ, resultado.getCnpj());
    }

    @Test
    void deveLancarExcecaoQuandoEmpresaNaoExiste() {
        when(empresaRepository.findById(CNPJ))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> empresaService.buscarPorCnpj(CNPJ)
        );
    }

    @Test
    void deveCadastrarEmpresaComCnpjNormalizado() {
        when(empresaRepository.existsById(CNPJ)).thenReturn(false);
        when(empresaRepository.existsByEmail("contato@techglobal.com"))
                .thenReturn(false);

        when(empresaRepository.save(any(Empresa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Empresa resultado = empresaService.cadastrar(
                "12.345.678/0001-99",
                "Tech Global",
                "contato@techglobal.com",
                "Brasil",
                "PE",
                "50000-000",
                "Tecnologia"
        );

        assertEquals(CNPJ, resultado.getCnpj());

        verify(empresaRepository).save(any(Empresa.class));
    }

    @Test
    void naoDeveCadastrarCnpjDuplicado() {
        when(empresaRepository.existsById(CNPJ)).thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> empresaService.cadastrar(
                        CNPJ, "Tech Global", "contato@techglobal.com",
                        null, null, null, null
                )
        );

        verify(empresaRepository, never()).save(any());
    }

    @Test
    void naoDeveCadastrarEmailDuplicado() {
        when(empresaRepository.existsById(CNPJ)).thenReturn(false);
        when(empresaRepository.existsByEmail("contato@techglobal.com"))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> empresaService.cadastrar(
                        CNPJ, "Tech Global", "contato@techglobal.com",
                        null, null, null, null
                )
        );

        verify(empresaRepository, never()).save(any());
    }

    @Test
    void deveAtualizarEmpresaSemAlterarCnpj() {
        Empresa empresa = criarEmpresa();

        when(empresaRepository.findById(CNPJ))
                .thenReturn(Optional.of(empresa));

        when(empresaRepository.findByEmail("novo@techglobal.com"))
                .thenReturn(Optional.empty());

        when(empresaRepository.save(any(Empresa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Empresa resultado = empresaService.atualizar(
                CNPJ,
                "Tech Global Atualizada",
                "novo@techglobal.com",
                "Brasil",
                "SP",
                "01000-000",
                "Nova descrição"
        );

        assertEquals(CNPJ, resultado.getCnpj());
        assertEquals("Tech Global Atualizada", resultado.getNome());
        assertEquals("novo@techglobal.com", resultado.getEmail());
    }

    @Test
    void naoDeveAceitarCnpjInvalido() {
        assertThrows(
                IllegalArgumentException.class,
                () -> empresaService.buscarPorCnpj("12345")
        );
    }
}