package com.zg.acelera.msnucleo.controller;

import com.zg.acelera.msnucleo.client.CompetenciasClient;
import com.zg.acelera.msnucleo.dto.CompetenciaResponseDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CompetenciasIntegracaoControllerTest {

    private MockMvc mockMvc;
    private CompetenciasClient competenciasClient;

    @BeforeEach
    void configurar() {
        competenciasClient = mock(CompetenciasClient.class);

        CompetenciasIntegracaoController controller =
                new CompetenciasIntegracaoController(competenciasClient);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void deveEncaminharConsultaEmLoteAoMsCompetencias() throws Exception {
        Map<String, List<CompetenciaResponseDTO>> resposta =
                new LinkedHashMap<>();

        resposta.put(
                "12345678900",
                List.of(new CompetenciaResponseDTO(1L, "Java"))
        );

        resposta.put("98765432100", List.of());

        when(competenciasClient.consultarPorCandidatosEmLote(
                List.of("12345678900", "98765432100")
        )).thenReturn(resposta);

        mockMvc.perform(
                        post("/api/integracao/competencias/candidatos/consulta-lote")
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
                        jsonPath("$['12345678900'][0].nome")
                                .value("Java")
                )
                .andExpect(
                        jsonPath("$['98765432100']")
                                .isEmpty()
                );

        verify(competenciasClient)
                .consultarPorCandidatosEmLote(
                        List.of("12345678900", "98765432100")
                );
    }

    @Test
    void deveConsultarCompetenciasDeUmaEmpresa() throws Exception {
        when(competenciasClient.listarPorEmpresa("12345678000199"))
                .thenReturn(List.of(
                        new CompetenciaResponseDTO(2L, "Spring")
                ));

        mockMvc.perform(
                        get("/api/integracao/competencias/empresas/12345678000199/competencias")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idCompetencia").value(2))
                .andExpect(jsonPath("$[0].nome").value("Spring"));

        verify(competenciasClient)
                .listarPorEmpresa("12345678000199");
    }

    @Test
    void deveConsultarCompetenciasDeUmaVaga() throws Exception {
        when(competenciasClient.listarPorVaga(10L))
                .thenReturn(List.of(
                        new CompetenciaResponseDTO(3L, "PostgreSQL")
                ));

        mockMvc.perform(
                        get("/api/integracao/competencias/vagas/10/competencias")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("PostgreSQL"));

        verify(competenciasClient).listarPorVaga(10L);
    }
}