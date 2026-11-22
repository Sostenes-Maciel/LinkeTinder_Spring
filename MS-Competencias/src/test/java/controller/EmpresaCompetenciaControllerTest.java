package com.zg.acelera.mscompetencias.controller;

import com.zg.acelera.mscompetencias.dto.CompetenciaResponse;
import com.zg.acelera.mscompetencias.exception.GlobalExceptionHandler;
import com.zg.acelera.mscompetencias.service.EmpresaCompetenciaService;

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

@WebMvcTest(EmpresaCompetenciaController.class)
@Import(GlobalExceptionHandler.class)
class EmpresaCompetenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmpresaCompetenciaService associacaoService;

    private final String cnpj = "12345678000195";

    @Test
    void deveAssociarCompetenciasAEmpresa() throws Exception {

        when(associacaoService.associar(
                eq(cnpj),
                eq(List.of("Java", "Spring"))
        )).thenReturn(List.of(
                new CompetenciaResponse(1L, "Java"),
                new CompetenciaResponse(2L, "Spring")
        ));

        mockMvc.perform(post("/api/empresas/{cnpj}/competencias", cnpj)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "competencias": ["Java", "Spring"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Java"))
                .andExpect(jsonPath("$[1].nome").value("Spring"));
    }

    @Test
    void deveListarCompetenciasDaEmpresa() throws Exception {

        when(associacaoService.listarPorEmpresa(cnpj))
                .thenReturn(List.of(
                        new CompetenciaResponse(1L, "Java")
                ));

        mockMvc.perform(get(
                        "/api/empresas/{cnpj}/competencias",
                        cnpj
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Java"));
    }

    @Test
    void deveRetornarListaVaziaQuandoEmpresaNaoPossuirCompetencias()
            throws Exception {

        when(associacaoService.listarPorEmpresa(cnpj))
                .thenReturn(List.of());

        mockMvc.perform(get(
                        "/api/empresas/{cnpj}/competencias",
                        cnpj
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deveRemoverAssociacaoDaEmpresa() throws Exception {

        doNothing().when(associacaoService)
                .removerAssociacao(cnpj, 1L);

        mockMvc.perform(delete(
                        "/api/empresas/{cnpj}/competencias/{id}",
                        cnpj,
                        1L
                ))
                .andExpect(status().isNoContent());

        verify(associacaoService)
                .removerAssociacao(cnpj, 1L);
    }

    @Test
    void deveRetornar400QuandoCnpjForInvalido() throws Exception {

        when(associacaoService.associar(
                eq(cnpj),
                anyList()
        )).thenThrow(new IllegalArgumentException(
                "O CNPJ deve possuir exatamente 14 dígitos."
        ));

        mockMvc.perform(post("/api/empresas/{cnpj}/competencias", cnpj)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "competencias": ["Java"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("O CNPJ deve possuir exatamente 14 dígitos."));
    }

    @Test
    void deveRetornar404QuandoAssociacaoNaoExistir() throws Exception {

        doThrow(new EntityNotFoundException(
                "Associação entre empresa e competência não encontrada."
        )).when(associacaoService).removerAssociacao(cnpj, 99L);

        mockMvc.perform(delete(
                        "/api/empresas/{cnpj}/competencias/{id}",
                        cnpj,
                        99L
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem")
                        .value("Associação entre empresa e competência não encontrada."));
    }
}