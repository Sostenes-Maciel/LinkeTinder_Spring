package com.zg.acelera.mscompetencias.controller;

import com.zg.acelera.mscompetencias.dto.CompetenciaResponse;
import com.zg.acelera.mscompetencias.exception.GlobalExceptionHandler;
import com.zg.acelera.mscompetencias.service.CandidatoCompetenciaService;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CandidatoCompetenciaController.class)
@Import(GlobalExceptionHandler.class)
class CandidatoCompetenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CandidatoCompetenciaService associacaoService;

    private final String cpf = "12345678900";

    @Test
    void deveAssociarCompetenciasAoCandidato() throws Exception {

        when(associacaoService.associar(
                eq(cpf),
                eq(List.of("Java", "SQL"))
        )).thenReturn(List.of(
                new CompetenciaResponse(1L, "Java"),
                new CompetenciaResponse(2L, "SQL")
        ));

        mockMvc.perform(post("/api/candidatos/{cpf}/competencias", cpf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "competencias": ["Java", "SQL"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Java"))
                .andExpect(jsonPath("$[1].nome").value("SQL"));
    }

    @Test
    void deveListarCompetenciasDoCandidato() throws Exception {

        when(associacaoService.listarPorCandidato(cpf))
                .thenReturn(List.of(
                        new CompetenciaResponse(1L, "Java")
                ));

        mockMvc.perform(get(
                        "/api/candidatos/{cpf}/competencias",
                        cpf
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Java"));
    }

    @Test
    void deveRetornarListaVaziaQuandoCandidatoNaoPossuirCompetencias()
            throws Exception {

        when(associacaoService.listarPorCandidato(cpf))
                .thenReturn(List.of());

        mockMvc.perform(get(
                        "/api/candidatos/{cpf}/competencias",
                        cpf
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deveRemoverAssociacaoDoCandidato() throws Exception {

        doNothing().when(associacaoService)
                .removerAssociacao(cpf, 1L);

        mockMvc.perform(delete(
                        "/api/candidatos/{cpf}/competencias/{id}",
                        cpf,
                        1L
                ))
                .andExpect(status().isNoContent());

        verify(associacaoService)
                .removerAssociacao(cpf, 1L);
    }

    @Test
    void deveRetornar400QuandoCpfForInvalido() throws Exception {

        when(associacaoService.associar(
                eq(cpf),
                anyList()
        )).thenThrow(new IllegalArgumentException(
                "O CPF deve possuir exatamente 11 dígitos."
        ));

        mockMvc.perform(post("/api/candidatos/{cpf}/competencias", cpf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "competencias": ["Java"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("O CPF deve possuir exatamente 11 dígitos."));
    }

    @Test
    void deveRetornar404QuandoAssociacaoNaoExistir() throws Exception {

        doThrow(new EntityNotFoundException(
                "Associação entre candidato e competência não encontrada."
        )).when(associacaoService).removerAssociacao(cpf, 99L);

        mockMvc.perform(delete(
                        "/api/candidatos/{cpf}/competencias/{id}",
                        cpf,
                        99L
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem")
                        .value("Associação entre candidato e competência não encontrada."));
    }
}