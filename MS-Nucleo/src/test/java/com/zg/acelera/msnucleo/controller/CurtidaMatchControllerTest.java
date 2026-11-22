package com.zg.acelera.msnucleo.controller;

import com.zg.acelera.msnucleo.model.MatchVaga;
import com.zg.acelera.msnucleo.service.CurtidaService;
import com.zg.acelera.msnucleo.service.MatchVagaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CurtidaMatchController.class)
class CurtidaMatchControllerTest {

    private static final String CPF = "12345678901";
    private static final String CNPJ = "12345678000199";
    private static final Long ID_VAGA = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurtidaService curtidaService;

    @MockitoBean
    private MatchVagaService matchVagaService;

    @Test
    void deveRegistrarCurtidaDeCandidatoSemMatch() throws Exception {

        when(matchVagaService.processarPossivelMatch(CPF, ID_VAGA))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/api/curtidas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "autor": "CANDIDATO",
                                    "cpfCandidato": "12345678901",
                                    "idVaga": 10
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensagem")
                        .value("Curtida registrada com sucesso."))
                .andExpect(jsonPath("$.match").value(false))
                .andExpect(jsonPath("$.idMatch").value(org.hamcrest.Matchers.nullValue()));

        verify(curtidaService).curtirVaga(CPF, ID_VAGA);
        verify(matchVagaService).processarPossivelMatch(CPF, ID_VAGA);
    }

    @Test
    void deveRegistrarCurtidaDeEmpresaEEncontrarMatch() throws Exception {

        MatchVaga match = mock(MatchVaga.class);

        when(matchVagaService.processarPossivelMatch(CPF, ID_VAGA))
                .thenReturn(Optional.of(match));

        when(match.getIdMatch()).thenReturn(50L);

        mockMvc.perform(
                        post("/api/curtidas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "autor": "EMPRESA",
                                    "cpfCandidato": "12345678901",
                                    "cnpjEmpresa": "12345678000199",
                                    "idVaga": 10
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensagem")
                        .value("Match encontrado com sucesso!"))
                .andExpect(jsonPath("$.match").value(true))
                .andExpect(jsonPath("$.idMatch").value(50));

        verify(curtidaService).curtirCandidato(CNPJ, CPF, ID_VAGA);
        verify(matchVagaService).processarPossivelMatch(CPF, ID_VAGA);
    }

    @Test
    void deveRetornar400QuandoAutorForInvalido() throws Exception {

        mockMvc.perform(
                        post("/api/curtidas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "autor": "RECRUTADOR",
                                    "cpfCandidato": "12345678901",
                                    "idVaga": 10
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("Informe um autor válido: CANDIDATO ou EMPRESA."));
    }

    @Test
    void deveRetornar400QuandoCurtidaForDuplicada() throws Exception {

        doThrow(new IllegalArgumentException("O candidato já curtiu esta vaga."))
                .when(curtidaService)
                .curtirVaga(CPF, ID_VAGA);

        mockMvc.perform(
                        post("/api/curtidas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "autor": "CANDIDATO",
                                    "cpfCandidato": "12345678901",
                                    "idVaga": 10
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("O candidato já curtiu esta vaga."));
    }

    @Test
    void deveRemoverCurtidaDeCandidatoERetornar204() throws Exception {

        mockMvc.perform(
                        delete("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "autor": "CANDIDATO",
                                    "cpfCandidato": "12345678901",
                                    "idVaga": 10
                                }
                                """)
                )
                .andExpect(status().isNoContent());

        verify(curtidaService).removerCurtidaCandidato(CPF, ID_VAGA);
    }

    @Test
    void deveRemoverCurtidaDeEmpresaERetornar204() throws Exception {

        mockMvc.perform(
                        delete("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "autor": "EMPRESA",
                                    "cpfCandidato": "12345678901",
                                    "cnpjEmpresa": "12345678000199",
                                    "idVaga": 10
                                }
                                """)
                )
                .andExpect(status().isNoContent());

        verify(curtidaService).removerCurtidaEmpresa(CNPJ, CPF, ID_VAGA);
    }

    @Test
    void deveRetornar409QuandoTentarRemoverCurtidaAposMatch() throws Exception {

        doThrow(new IllegalStateException(
                "A curtida não pode ser removida porque já existe um Match."
        ))
                .when(curtidaService)
                .removerCurtidaCandidato(CPF, ID_VAGA);

        mockMvc.perform(
                        delete("/api/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "autor": "CANDIDATO",
                                    "cpfCandidato": "12345678901",
                                    "idVaga": 10
                                }
                                """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem")
                        .value("A curtida não pode ser removida porque já existe um Match."));
    }
}