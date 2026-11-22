package com.zg.acelera.mscompetencias.service;

import com.zg.acelera.mscompetencias.dto.CompetenciaResponse;
import com.zg.acelera.mscompetencias.model.Competencia;
import com.zg.acelera.mscompetencias.model.VagaCompetencia;
import com.zg.acelera.mscompetencias.model.id.VagaCompetenciaId;
import com.zg.acelera.mscompetencias.repository.VagaCompetenciaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VagaCompetenciaServiceTest {

    @Mock
    private VagaCompetenciaRepository associacaoRepository;

    @Mock
    private CompetenciaService competenciaService;

    @InjectMocks
    private VagaCompetenciaService associacaoService;

    @Test
    void deveAssociarCompetenciaComVaga() {

        Competencia competencia = mock(Competencia.class);

        when(competencia.getIdCompetencia()).thenReturn(30L);
        when(competencia.getNome()).thenReturn("PostgreSQL");

        when(competenciaService.buscarOuCriar("PostgreSQL"))
                .thenReturn(competencia);

        VagaCompetenciaId id =
                new VagaCompetenciaId(5L, 30L);

        when(associacaoRepository.existsById(id))
                .thenReturn(false);

        when(associacaoRepository.save(any(VagaCompetencia.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        List<CompetenciaResponse> resultado =
                associacaoService.associar(
                        5L,
                        List.of("PostgreSQL")
                );

        assertEquals(1, resultado.size());
        assertEquals("PostgreSQL", resultado.get(0).nome());
        assertEquals(30L, resultado.get(0).idCompetencia());

        verify(associacaoRepository).save(
                any(VagaCompetencia.class)
        );
    }

    @Test
    void naoDeveDuplicarAssociacaoExistente() {

        Competencia competencia = mock(Competencia.class);

        when(competencia.getIdCompetencia()).thenReturn(30L);
        when(competencia.getNome()).thenReturn("PostgreSQL");

        when(competenciaService.buscarOuCriar("PostgreSQL"))
                .thenReturn(competencia);

        VagaCompetenciaId id =
                new VagaCompetenciaId(5L, 30L);

        when(associacaoRepository.existsById(id))
                .thenReturn(true);

        associacaoService.associar(
                5L,
                List.of("PostgreSQL")
        );

        verify(associacaoRepository, never())
                .save(any(VagaCompetencia.class));
    }

    @Test
    void naoDeveAssociarComIdVagaInvalido() {

        assertThrows(
                IllegalArgumentException.class,
                () -> associacaoService.associar(
                        0L,
                        List.of("PostgreSQL")
                )
        );

        verifyNoInteractions(
                associacaoRepository,
                competenciaService
        );
    }
}