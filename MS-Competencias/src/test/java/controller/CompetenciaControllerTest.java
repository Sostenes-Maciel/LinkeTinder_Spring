package com.zg.acelera.mscompetencias.controller;

import com.zg.acelera.mscompetencias.exception.GlobalExceptionHandler;
import com.zg.acelera.mscompetencias.model.Competencia;
import com.zg.acelera.mscompetencias.service.CompetenciaService;

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

@WebMvcTest(CompetenciaController.class)
@Import(GlobalExceptionHandler.class)
class CompetenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompetenciaService competenciaService;

    @Test
    void deveCadastrarCompetencia() throws Exception {

        Competencia competencia = mock(Competencia.class);

        when(competencia.getIdCompetencia()).thenReturn(1L);
        when(competencia.getNome()).thenReturn("Java");

        when(competenciaService.criar("Java"))
                .thenReturn(competencia);

        mockMvc.perform(post("/api/competencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nome": "Java"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCompetencia").value(1))
                .andExpect(jsonPath("$.nome").value("Java"))
                .andExpect(header().exists("Location"));
    }

    @Test
    void deveListarTodasAsCompetencias() throws Exception {

        Competencia competencia = mock(Competencia.class);

        when(competencia.getIdCompetencia()).thenReturn(1L);
        when(competencia.getNome()).thenReturn("Java");

        when(competenciaService.listarTodas())
                .thenReturn(List.of(competencia));

        mockMvc.perform(get("/api/competencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Java"));
    }

    @Test
    void deveBuscarCompetenciaPorId() throws Exception {

        Competencia competencia = mock(Competencia.class);

        when(competencia.getIdCompetencia()).thenReturn(1L);
        when(competencia.getNome()).thenReturn("Java");

        when(competenciaService.buscarPorId(1L))
                .thenReturn(competencia);

        mockMvc.perform(get("/api/competencias/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCompetencia").value(1))
                .andExpect(jsonPath("$.nome").value("Java"));
    }

    @Test
    void deveRetornar404QuandoCompetenciaNaoExistir() throws Exception {

        when(competenciaService.buscarPorId(99L))
                .thenThrow(new EntityNotFoundException(
                        "Competência não encontrada: 99"
                ));

        mockMvc.perform(get("/api/competencias/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem")
                        .value("Competência não encontrada: 99"));
    }

    @Test
    void deveAtualizarCompetencia() throws Exception {

        Competencia competencia = mock(Competencia.class);

        when(competencia.getIdCompetencia()).thenReturn(1L);
        when(competencia.getNome()).thenReturn("Spring");

        when(competenciaService.atualizar(1L, "Spring"))
                .thenReturn(competencia);

        mockMvc.perform(put("/api/competencias/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nome": "Spring"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Spring"));
    }

    @Test
    void deveRetornar400QuandoNomeForDuplicado() throws Exception {

        when(competenciaService.criar("Java"))
                .thenThrow(new IllegalArgumentException(
                        "Já existe uma competência com esse nome."
                ));

        mockMvc.perform(post("/api/competencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nome": "Java"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("Já existe uma competência com esse nome."));
    }

    @Test
    void deveExcluirCompetencia() throws Exception {

        doNothing().when(competenciaService).excluir(1L);

        mockMvc.perform(delete("/api/competencias/1"))
                .andExpect(status().isNoContent());

        verify(competenciaService).excluir(1L);
    }
}