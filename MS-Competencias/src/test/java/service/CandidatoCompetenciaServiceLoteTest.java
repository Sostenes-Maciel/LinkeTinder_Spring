package com.zg.acelera.mscompetencias.service;

import com.zg.acelera.mscompetencias.dto.CompetenciaResponse;
import com.zg.acelera.mscompetencias.model.CandidatoCompetencia;
import com.zg.acelera.mscompetencias.model.Competencia;
import com.zg.acelera.mscompetencias.repository.CandidatoCompetenciaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CandidatoCompetenciaServiceLoteTest {

    private CandidatoCompetenciaRepository associacaoRepository;
    private CompetenciaService competenciaService;
    private CandidatoCompetenciaService service;

    @BeforeEach
    void configurar() {
        associacaoRepository = mock(CandidatoCompetenciaRepository.class);
        competenciaService = mock(CompetenciaService.class);

        service = new CandidatoCompetenciaService(
                associacaoRepository,
                competenciaService
        );
    }

    @Test
    void deveAgruparCompetenciasPorCpf() {
        String cpf = "12345678900";

        Competencia competencia = mock(Competencia.class);
        CandidatoCompetencia associacao = mock(CandidatoCompetencia.class);

        when(competencia.getIdCompetencia()).thenReturn(1L);
        when(competencia.getNome()).thenReturn("Java");

        when(associacao.getCpfCandidato()).thenReturn(cpf);
        when(associacao.getCompetencia()).thenReturn(competencia);

        when(associacaoRepository.buscarPorCpfsCandidatos(List.of(cpf)))
                .thenReturn(List.of(associacao));

        Map<String, List<CompetenciaResponse>> resultado =
                service.listarPorCandidatos(List.of(cpf));

        assertEquals(1, resultado.size());
        assertEquals(
                List.of(new CompetenciaResponse(1L, "Java")),
                resultado.get(cpf)
        );

        verify(associacaoRepository, times(1))
                .buscarPorCpfsCandidatos(List.of(cpf));

        verify(associacaoRepository, never())
                .buscarPorCpfCandidato(anyString());
    }

    @Test
    void deveNormalizarERemoverCpfsDuplicados() {
        String cpf1 = "12345678900";
        String cpf2 = "98765432100";

        List<String> cpfsRecebidos = List.of(
                "123.456.789-00",
                cpf1,
                "987.654.321-00"
        );

        when(associacaoRepository.buscarPorCpfsCandidatos(
                List.of(cpf1, cpf2)
        )).thenReturn(List.of());

        Map<String, List<CompetenciaResponse>> resultado =
                service.listarPorCandidatos(cpfsRecebidos);

        assertEquals(
                List.of(cpf1, cpf2),
                new ArrayList<>(resultado.keySet())
        );

        assertTrue(resultado.get(cpf1).isEmpty());
        assertTrue(resultado.get(cpf2).isEmpty());

        verify(associacaoRepository)
                .buscarPorCpfsCandidatos(List.of(cpf1, cpf2));
    }

    @Test
    void deveRetornarListaVaziaParaCandidatoSemCompetencias() {
        String cpf = "12345678900";

        when(associacaoRepository.buscarPorCpfsCandidatos(List.of(cpf)))
                .thenReturn(List.of());

        Map<String, List<CompetenciaResponse>> resultado =
                service.listarPorCandidatos(List.of(cpf));

        assertTrue(resultado.containsKey(cpf));
        assertNotNull(resultado.get(cpf));
        assertTrue(resultado.get(cpf).isEmpty());
    }

    @Test
    void deveRetornarMapaVazioQuandoListaDeCpfsEstiverVazia() {
        Map<String, List<CompetenciaResponse>> resultado =
                service.listarPorCandidatos(List.of());

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());

        verifyNoInteractions(associacaoRepository);
    }

    @Test
    void deveRejeitarListaNula() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.listarPorCandidatos(null)
        );

        verifyNoInteractions(associacaoRepository);
    }

    @Test
    void deveRejeitarMaisDeQuinhentosCpfs() {
        List<String> cpfs = IntStream.range(0, 501)
                .mapToObj(numero -> String.format("%011d", numero))
                .toList();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.listarPorCandidatos(cpfs)
        );

        verifyNoInteractions(associacaoRepository);
    }

    @Test
    void deveRejeitarCpfInvalidoNaLista() {
        List<String> cpfs = List.of(
                "12345678900",
                "12345"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.listarPorCandidatos(cpfs)
        );

        verifyNoInteractions(associacaoRepository);
    }
}