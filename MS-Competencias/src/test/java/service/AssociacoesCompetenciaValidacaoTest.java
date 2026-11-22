package com.zg.acelera.mscompetencias.service;

import com.zg.acelera.mscompetencias.model.Competencia;
import com.zg.acelera.mscompetencias.model.CandidatoCompetencia;
import com.zg.acelera.mscompetencias.model.EmpresaCompetencia;
import com.zg.acelera.mscompetencias.model.VagaCompetencia;

import com.zg.acelera.mscompetencias.model.id.CandidatoCompetenciaId;
import com.zg.acelera.mscompetencias.model.id.EmpresaCompetenciaId;
import com.zg.acelera.mscompetencias.model.id.VagaCompetenciaId;

import com.zg.acelera.mscompetencias.repository.CandidatoCompetenciaRepository;
import com.zg.acelera.mscompetencias.repository.EmpresaCompetenciaRepository;
import com.zg.acelera.mscompetencias.repository.VagaCompetenciaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AssociacoesCompetenciaValidacaoTest {

    @Mock
    private CompetenciaService competenciaService;

    @Mock
    private CandidatoCompetenciaRepository candidatoRepository;

    @Mock
    private EmpresaCompetenciaRepository empresaRepository;

    @Mock
    private VagaCompetenciaRepository vagaRepository;

    @InjectMocks
    private CandidatoCompetenciaService candidatoService;

    @InjectMocks
    private EmpresaCompetenciaService empresaService;

    @InjectMocks
    private VagaCompetenciaService vagaService;

    private Competencia criarCompetenciaMock(Long id, String nome) {

        Competencia competencia = mock(Competencia.class);

        when(competencia.getIdCompetencia()).thenReturn(id);
        when(competencia.getNome()).thenReturn(nome);

        return competencia;
    }

    // CANDIDATO

    @Test
    void deveIgnorarCompetenciasDuplicadasNaAssociacaoDoCandidato() {

        Competencia competencia = criarCompetenciaMock(1L, "Java");

        when(competenciaService.buscarOuCriar("Java"))
                .thenReturn(competencia);

        CandidatoCompetenciaId id =
                new CandidatoCompetenciaId("12345678900", 1L);

        when(candidatoRepository.existsById(id))
                .thenReturn(false);

        when(candidatoRepository.save(any(CandidatoCompetencia.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resultado = candidatoService.associar(
                "123.456.789-00",
                List.of("Java", "Java", "Java")
        );

        assertEquals(1, resultado.size());

        verify(candidatoRepository, times(1))
                .existsById(id);

        verify(candidatoRepository, times(1))
                .save(any(CandidatoCompetencia.class));
    }

    @Test
    void naoDeveAssociarListaVaziaAoCandidato() {

        assertThrows(
                IllegalArgumentException.class,
                () -> candidatoService.associar(
                        "12345678900",
                        List.of()
                )
        );

        verifyNoInteractions(
                candidatoRepository,
                competenciaService
        );
    }

    // EMPRESA

    @Test
    void deveIgnorarCompetenciasDuplicadasNaAssociacaoDaEmpresa() {

        Competencia competencia = criarCompetenciaMock(2L, "Spring");

        when(competenciaService.buscarOuCriar("Spring"))
                .thenReturn(competencia);

        EmpresaCompetenciaId id =
                new EmpresaCompetenciaId("12345678000195", 2L);

        when(empresaRepository.existsById(id))
                .thenReturn(false);

        when(empresaRepository.save(any(EmpresaCompetencia.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resultado = empresaService.associar(
                "12.345.678/0001-95",
                List.of("Spring", "Spring", "Spring")
        );

        assertEquals(1, resultado.size());

        ArgumentCaptor<EmpresaCompetencia> captor =
                ArgumentCaptor.forClass(EmpresaCompetencia.class);

        verify(empresaRepository).save(captor.capture());

        assertEquals(
                "12345678000195",
                captor.getValue().getCnpjEmpresa()
        );
    }

    @Test
    void naoDeveAssociarListaVaziaAEmpresa() {

        assertThrows(
                IllegalArgumentException.class,
                () -> empresaService.associar(
                        "12345678000195",
                        List.of()
                )
        );

        verifyNoInteractions(
                empresaRepository,
                competenciaService
        );
    }

    // VAGA

    @Test
    void deveIgnorarCompetenciasDuplicadasNaAssociacaoDaVaga() {

        Competencia competencia =
                criarCompetenciaMock(3L, "PostgreSQL");

        when(competenciaService.buscarOuCriar("PostgreSQL"))
                .thenReturn(competencia);

        VagaCompetenciaId id =
                new VagaCompetenciaId(10L, 3L);

        when(vagaRepository.existsById(id))
                .thenReturn(false);

        when(vagaRepository.save(any(VagaCompetencia.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resultado = vagaService.associar(
                10L,
                List.of("PostgreSQL", "PostgreSQL", "PostgreSQL")
        );

        assertEquals(1, resultado.size());

        verify(vagaRepository, times(1))
                .existsById(id);

        verify(vagaRepository, times(1))
                .save(any(VagaCompetencia.class));
    }

    @Test
    void naoDeveAssociarListaVaziaAVaga() {

        assertThrows(
                IllegalArgumentException.class,
                () -> vagaService.associar(
                        10L,
                        List.of()
                )
        );

        verifyNoInteractions(
                vagaRepository,
                competenciaService
        );
    }
}