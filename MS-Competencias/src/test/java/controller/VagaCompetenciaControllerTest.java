package com.zg.acelera.mscompetencias.controller;

import com.zg.acelera.mscompetencias.dto.CompetenciaResponse;
import com.zg.acelera.mscompetencias.exception.GlobalExceptionHandler;
import com.zg.acelera.mscompetencias.service.VagaCompetenciaService;

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

@WebMvcTest(VagaCompetenciaController.class)
@Import(GlobalExceptionHandler.class)
class VagaCompetenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VagaCompetenciaService associacaoService;

    private final Long idVaga = 10L;

    @Test
    void deveAssociarCompetenciasAVaga() throws Exception {

        when(associacaoService.associar(
                eq(idVaga),
                eq(List.of("Java", "PostgreSQL"))
        )).thenReturn(List.of(
                new CompetenciaResponse(1L, "Java"),
                new CompetenciaResponse(2L, "PostgreSQL")
        ));

        mockMvc.perform(post(
                        "/api/vagas/{idVaga}/competencias",
                        idVaga
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "competencias": ["Java", "PostgreSQL"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Java"))
                .andExpect(jsonPath("$[1].nome").value("PostgreSQL"));
    }

    @Test
    void deveListarCompetenciasDaVaga() throws Exception {

        when(associacaoService.listarPorVaga(idVaga))
                .thenReturn(List.of(
                        new CompetenciaResponse(1L, "Java")
                ));

        mockMvc.perform(get(
                        "/api/vagas/{idVaga}/competencias",
                        idVaga
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Java"));
    }

    @Test
    void deveRetornarListaVaziaQuandoVagaNaoPossuirCompetencias()
            throws Exception {

        when(associacaoService.listarPorVaga(idVaga))
                .thenReturn(List.of());

        mockMvc.perform(get(
                        "/api/vagas/{idVaga}/competencias",
                        idVaga
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deveRemoverAssociacaoDaVaga() throws Exception {

        doNothing().when(associacaoService)
                .removerAssociacao(idVaga, 1L);

        mockMvc.perform(delete(
                        "/api/vagas/{idVaga}/competencias/{idCompetencia}",
                        idVaga,
                        1L
                ))
                .andExpect(status().isNoContent());

        verify(associacaoService)
                .removerAssociacao(idVaga, 1L);
    }

    @Test
    void deveRetornar400QuandoIdVagaForInvalido() throws Exception {

        when(associacaoService.associar(
                eq(0L),
                anyList()
        )).thenThrow(new IllegalArgumentException(
                "O identificador da vaga é inválido."
        ));

        mockMvc.perform(post(
                        "/api/vagas/0/competencias"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "competencias": ["Java"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("O identificador da vaga é inválido."));
    }

    @Test
    void deveRetornar404QuandoAssociacaoNaoExistir() throws Exception {

        doThrow(new EntityNotFoundException(
                "Associação entre vaga e competência não encontrada."
        )).when(associacaoService).removerAssociacao(idVaga, 99L);

        mockMvc.perform(delete(
                        "/api/vagas/{idVaga}/competencias/{idCompetencia}",
                        idVaga,
                        99L
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem")
                        .value("Associação entre vaga e competência não encontrada."));
    }
}