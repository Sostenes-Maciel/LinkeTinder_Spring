package com.zg.acelera.mscompetencias.service;

import com.zg.acelera.mscompetencias.model.Competencia;
import com.zg.acelera.mscompetencias.repository.CompetenciaRepository;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class CompetenciaServiceTest {

    @Mock
    private CompetenciaRepository competenciaRepository;

    @InjectMocks
    private CompetenciaService competenciaService;

    @Test
    void deveListarTodasAsCompetencias() {

        List<Competencia> competencias = List.of(
                new Competencia("Java"),
                new Competencia("SQL")
        );

        when(competenciaRepository.findAll()).thenReturn(competencias);

        List<Competencia> resultado = competenciaService.listarTodas();

        assertEquals(2, resultado.size());
        assertEquals("Java", resultado.get(0).getNome());

        verify(competenciaRepository).findAll();
    }

    @Test
    void deveBuscarCompetenciaPorId() {

        Competencia competencia = new Competencia("Java");

        when(competenciaRepository.findById(1L))
                .thenReturn(Optional.of(competencia));

        Competencia resultado = competenciaService.buscarPorId(1L);

        assertEquals("Java", resultado.getNome());
    }

    @Test
    void deveLancarExcecaoQuandoCompetenciaNaoExistir() {

        when(competenciaRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> competenciaService.buscarPorId(99L)
        );
    }

    @Test
    void deveCriarCompetenciaComNomeNormalizado() {

        when(competenciaRepository.findByNome("Java"))
                .thenReturn(Optional.empty());

        when(competenciaRepository.save(any(Competencia.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Competencia resultado = competenciaService.criar(" Java ");

        assertEquals("Java", resultado.getNome());

        verify(competenciaRepository).save(any(Competencia.class));
    }

    @Test
    void naoDeveCriarCompetenciaDuplicada() {

        Competencia existente = new Competencia("Java");

        when(competenciaRepository.findByNome("Java"))
                .thenReturn(Optional.of(existente));

        assertThrows(
                IllegalArgumentException.class,
                () -> competenciaService.criar("Java")
        );

        verify(competenciaRepository, never())
                .save(any(Competencia.class));
    }

    @Test
    void deveReutilizarCompetenciaQuandoJaExistir() {

        Competencia existente = new Competencia("Java");

        when(competenciaRepository.findByNome("Java"))
                .thenReturn(Optional.of(existente));

        Competencia resultado =
                competenciaService.buscarOuCriar("Java");

        assertSame(existente, resultado);

        verify(competenciaRepository, never())
                .save(any(Competencia.class));
    }

    @Test
    void deveCriarCompetenciaQuandoNaoExistir() {

        when(competenciaRepository.findByNome("Spring"))
                .thenReturn(Optional.empty());

        when(competenciaRepository.save(any(Competencia.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Competencia resultado =
                competenciaService.buscarOuCriar("Spring");

        assertEquals("Spring", resultado.getNome());

        verify(competenciaRepository).save(any(Competencia.class));
    }

    @Test
    void deveAtualizarCompetencia() {

        Competencia competencia = new Competencia("Java");

        when(competenciaRepository.findById(1L))
                .thenReturn(Optional.of(competencia));

        when(competenciaRepository.findByNome("Spring"))
                .thenReturn(Optional.empty());

        when(competenciaRepository.save(competencia))
                .thenReturn(competencia);

        Competencia resultado =
                competenciaService.atualizar(1L, " Spring ");

        assertEquals("Spring", resultado.getNome());

        verify(competenciaRepository).save(competencia);
    }

    @Test
    void naoDeveAtualizarParaNomeJaUtilizado() {

        Competencia atual = new Competencia("Java");

        Competencia outra = mock(Competencia.class);

        when(outra.getIdCompetencia()).thenReturn(2L);

        when(competenciaRepository.findById(1L))
                .thenReturn(Optional.of(atual));

        when(competenciaRepository.findByNome("Spring"))
                .thenReturn(Optional.of(outra));

        assertThrows(
                IllegalArgumentException.class,
                () -> competenciaService.atualizar(1L, "Spring")
        );

        verify(competenciaRepository, never())
                .save(any(Competencia.class));
    }

    @Test
    void deveExcluirCompetencia() {

        Competencia competencia = new Competencia("Java");

        when(competenciaRepository.findById(1L))
                .thenReturn(Optional.of(competencia));

        competenciaService.excluir(1L);

        verify(competenciaRepository).delete(competencia);
    }

    @Test
    void naoDeveExcluirCompetenciaInexistente() {

        when(competenciaRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> competenciaService.excluir(99L)
        );

        verify(competenciaRepository, never())
                .delete(any(Competencia.class));
    }
}