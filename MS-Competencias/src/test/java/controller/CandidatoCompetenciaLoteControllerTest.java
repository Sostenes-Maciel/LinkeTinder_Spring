package com.zg.acelera.mscompetencias.controller;

import com.zg.acelera.mscompetencias.dto.CompetenciaResponse;
import com.zg.acelera.mscompetencias.service.CandidatoCompetenciaService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CandidatoCompetenciaLoteControllerTest {

    private MockMvc mockMvc;
    private CandidatoCompetenciaService associacaoService;

    @BeforeEach
    void configurar() {
        associacaoService = mock(CandidatoCompetenciaService.class);

        CandidatoCompetenciaLoteController controller =
                new CandidatoCompetenciaLoteController(associacaoService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void deveConsultarCompetenciasDeVariosCandidatos() throws Exception {
        String cpf1 = "12345678900";
        String cpf2 = "98765432100";

        Map<String, List<CompetenciaResponse>> resposta =
                new LinkedHashMap<>();

        resposta.put(
                cpf1,
                List.of(new CompetenciaResponse(1L, "Java"))
        );

        resposta.put(cpf2, List.of());

        when(associacaoService.listarPorCandidatos(List.of(cpf1, cpf2)))
                .thenReturn(resposta);

        mockMvc.perform(
                        post("/api/competencias/candidatos/consulta-lote")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "cpfs": [
                                                "12345678900",
                                                "98765432100"
                                            ]
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$['12345678900'][0].idCompetencia")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$['12345678900'][0].nome")
                                .value("Java")
                )
                .andExpect(
                        jsonPath("$['98765432100']")
                                .isArray()
                )
                .andExpect(
                        jsonPath("$['98765432100']")
                                .isEmpty()
                );

        verify(associacaoService)
                .listarPorCandidatos(List.of(cpf1, cpf2));
    }
}