package com.zg.acelera.msnucleo.service;

import com.zg.acelera.msnucleo.model.Candidato;
import com.zg.acelera.msnucleo.repository.CandidatoRepository;
import com.zg.acelera.msnucleo.CandidatoService;
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
class CandidatoServiceTest {

    private static final String CPF = "12345678901";

    @Mock
    private CandidatoRepository candidatoRepository;

    @InjectMocks
    private CandidatoService candidatoService;

    private Candidato criarCandidato() {
        return new Candidato(
                CPF, "Ana", "ana@email.com",
                "Brasil", "PE", "50000-000",
                "Desenvolvedora", 25
        );
    }

    @Test
    void deveBuscarCandidatoNormalizandoCpf() {
        Candidato candidato = criarCandidato();

        when(candidatoRepository.findById(CPF))
                .thenReturn(Optional.of(candidato));

        Candidato resultado =
                candidatoService.buscarPorCpf("123.456.789-01");

        assertEquals(CPF, resultado.getCpf());
    }

    @Test
    void deveLancarExcecaoQuandoCandidatoNaoExiste() {
        when(candidatoRepository.findById(CPF))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> candidatoService.buscarPorCpf(CPF)
        );
    }

    @Test
    void deveCadastrarCandidatoComCpfNormalizado() {
        when(candidatoRepository.existsById(CPF)).thenReturn(false);
        when(candidatoRepository.existsByEmail("ana@email.com"))
                .thenReturn(false);

        when(candidatoRepository.save(any(Candidato.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Candidato resultado = candidatoService.cadastrar(
                "123.456.789-01",
                " Ana ",
                "ana@email.com",
                "Brasil",
                "PE",
                "50000-000",
                "Desenvolvedora",
                25
        );

        assertEquals(CPF, resultado.getCpf());
        assertEquals("Ana", resultado.getNome());
        verify(candidatoRepository).save(any(Candidato.class));
    }

    @Test
    void naoDeveCadastrarCpfDuplicado() {
        when(candidatoRepository.existsById(CPF)).thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> candidatoService.cadastrar(
                        CPF, "Ana", "ana@email.com",
                        null, null, null, null, 25
                )
        );

        verify(candidatoRepository, never()).save(any());
    }

    @Test
    void naoDeveCadastrarEmailDuplicado() {
        when(candidatoRepository.existsById(CPF)).thenReturn(false);
        when(candidatoRepository.existsByEmail("ana@email.com"))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> candidatoService.cadastrar(
                        CPF, "Ana", "ana@email.com",
                        null, null, null, null, 25
                )
        );

        verify(candidatoRepository, never()).save(any());
    }

    @Test
    void deveAtualizarDadosSemAlterarCpf() {
        Candidato candidato = criarCandidato();

        when(candidatoRepository.findById(CPF))
                .thenReturn(Optional.of(candidato));

        when(candidatoRepository.findByEmail("novo@email.com"))
                .thenReturn(Optional.empty());

        when(candidatoRepository.save(any(Candidato.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Candidato resultado = candidatoService.atualizar(
                CPF,
                "Ana Souza",
                "novo@email.com",
                "Brasil",
                "SP",
                "01000-000",
                "Nova descrição",
                26
        );

        assertEquals(CPF, resultado.getCpf());
        assertEquals("Ana Souza", resultado.getNome());
        assertEquals("novo@email.com", resultado.getEmail());
        assertEquals(26, resultado.getIdade());
    }

    @Test
    void naoDeveAtualizarComEmailDeOutroCandidato() {
        Candidato candidato = criarCandidato();

        Candidato outro = new Candidato(
                "98765432100", "Maria", "maria@email.com",
                "Brasil", "SP", null, null, 30
        );

        when(candidatoRepository.findById(CPF))
                .thenReturn(Optional.of(candidato));

        when(candidatoRepository.findByEmail("maria@email.com"))
                .thenReturn(Optional.of(outro));

        assertThrows(
                IllegalArgumentException.class,
                () -> candidatoService.atualizar(
                        CPF, "Ana", "maria@email.com",
                        null, null, null, null, 25
                )
        );

        verify(candidatoRepository, never()).save(any());
    }

    @Test
    void naoDeveAceitarCpfComQuantidadeInvalidaDeDigitos() {
        assertThrows(
                IllegalArgumentException.class,
                () -> candidatoService.buscarPorCpf("12345")
        );
    }
}