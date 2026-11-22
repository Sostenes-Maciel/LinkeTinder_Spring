package com.zg.acelera.mscompetencias.service;

import com.zg.acelera.mscompetencias.dto.CompetenciaResponse;
import com.zg.acelera.mscompetencias.model.Competencia;
import com.zg.acelera.mscompetencias.model.CandidatoCompetencia;
import com.zg.acelera.mscompetencias.model.id.CandidatoCompetenciaId;
import com.zg.acelera.mscompetencias.repository.CandidatoCompetenciaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidatoCompetenciaServiceTest {

    @Mock
    private CandidatoCompetenciaRepository associacaoRepository;

    @Mock
    private CompetenciaService competenciaService;

    @InjectMocks
    private CandidatoCompetenciaService associacaoService;

    @Test
    void deveAssociarCompetenciaComCpfNormalizado() {

        Competencia competencia = mock(Competencia.class);

        when(competencia.getIdCompetencia()).thenReturn(10L);
        when(competencia.getNome()).thenReturn("Java");

        when(competenciaService.buscarOuCriar("Java"))
                .thenReturn(competencia);

        CandidatoCompetenciaId id =
                new CandidatoCompetenciaId("12345678900", 10L);

        when(associacaoRepository.existsById(id))
                .thenReturn(false);

        when(associacaoRepository.save(any(CandidatoCompetencia.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        List<CompetenciaResponse> resultado =
                associacaoService.associar(
                        "123.456.789-00",
                        List.of("Java")
                );

        assertEquals(1, resultado.size());
        assertEquals("Java", resultado.get(0).nome());
        assertEquals(10L, resultado.get(0).idCompetencia());

        verify(associacaoRepository).save(
                any(CandidatoCompetencia.class)
        );

        verify(competenciaService).buscarOuCriar("Java");
    }

    @Test
    void naoDeveDuplicarAssociacaoExistente() {

        Competencia competencia = mock(Competencia.class);

        when(competencia.getIdCompetencia()).thenReturn(10L);
        when(competencia.getNome()).thenReturn("Java");

        when(competenciaService.buscarOuCriar("Java"))
                .thenReturn(competencia);

        CandidatoCompetenciaId id =
                new CandidatoCompetenciaId("12345678900", 10L);

        when(associacaoRepository.existsById(id))
                .thenReturn(true);

        associacaoService.associar(
                "12345678900",
                List.of("Java")
        );

        verify(associacaoRepository, never())
                .save(any(CandidatoCompetencia.class));
    }

    @Test
    void naoDeveAssociarComCpfInvalido() {

        assertThrows(
                IllegalArgumentException.class,
                () -> associacaoService.associar(
                        "123.456",
                        List.of("Java")
                )
        );

        verifyNoInteractions(
                associacaoRepository,
                competenciaService
        );
    }
}