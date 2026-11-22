package com.zg.acelera.msnucleo.controller;

import com.zg.acelera.msnucleo.model.Vaga;
import com.zg.acelera.msnucleo.service.VagaService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VagaController.class)
class VagaControllerTest {

    private static final String CNPJ = "12345678000199";
    private static final String TITULO = "Desenvolvedor Java";
    private static final String PAIS = "Brasil";
    private static final String ESTADO = "PE";
    private static final String DESCRICAO = "Desenvolvimento de aplicações Java";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VagaService vagaService;

    @Test
    void deveListarVagasSemExporDadosDaEmpresa() throws Exception {

        Vaga vaga = criarVagaMock(10L);

        when(vagaService.listarTodas()).thenReturn(List.of(vaga));

        mockMvc.perform(get("/api/vagas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idVaga").value(10))
                .andExpect(jsonPath("$[0].titulo").value(TITULO))
                .andExpect(jsonPath("$[0].pais").value(PAIS))
                .andExpect(jsonPath("$[0].estado").value(ESTADO))
                .andExpect(jsonPath("$[0].descricao").value(DESCRICAO))
                .andExpect(jsonPath("$[0].cnpjEmpresa").doesNotExist())
                .andExpect(jsonPath("$[0].empresa").doesNotExist());

        verify(vagaService).listarTodas();
    }

    @Test
    void deveBuscarVagaPorId() throws Exception {

        Vaga vaga = criarVagaMock(10L);

        when(vagaService.buscarPorId(10L)).thenReturn(vaga);

        mockMvc.perform(get("/api/vagas/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idVaga").value(10))
                .andExpect(jsonPath("$.titulo").value(TITULO))
                .andExpect(jsonPath("$.estado").value(ESTADO));

        verify(vagaService).buscarPorId(10L);
    }

    @Test
    void deveListarVagasDeUmaEmpresa() throws Exception {

        Vaga vaga = criarVagaMock(10L);

        when(vagaService.listarPorEmpresa(CNPJ)).thenReturn(List.of(vaga));

        mockMvc.perform(get("/api/vagas/empresa/" + CNPJ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idVaga").value(10))
                .andExpect(jsonPath("$[0].titulo").value(TITULO))
                .andExpect(jsonPath("$[0].cnpjEmpresa").doesNotExist());

        verify(vagaService).listarPorEmpresa(CNPJ);
    }

    @Test
    void deveCadastrarVagaERetornar201() throws Exception {

        Vaga vaga = criarVagaMock(10L);

        when(vagaService.cadastrar(
                TITULO,
                PAIS,
                ESTADO,
                DESCRICAO,
                CNPJ
        )).thenReturn(vaga);

        mockMvc.perform(
                        post("/api/vagas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "titulo": "Desenvolvedor Java",
                                    "pais": "Brasil",
                                    "estado": "PE",
                                    "descricao": "Desenvolvimento de aplicações Java",
                                    "cnpjEmpresa": "12345678000199"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idVaga").value(10))
                .andExpect(jsonPath("$.titulo").value(TITULO))
                .andExpect(jsonPath("$.estado").value(ESTADO));

        verify(vagaService).cadastrar(
                TITULO,
                PAIS,
                ESTADO,
                DESCRICAO,
                CNPJ
        );
    }

    @Test
    void deveAtualizarVagaERetornar200() throws Exception {

        Vaga vaga = criarVagaMock(10L);

        when(vagaService.atualizar(
                10L,
                CNPJ,
                TITULO,
                PAIS,
                ESTADO,
                DESCRICAO
        )).thenReturn(vaga);

        mockMvc.perform(
                        put("/api/vagas/10")
                                .param("cnpjEmpresa", CNPJ)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "titulo": "Desenvolvedor Java",
                                    "pais": "Brasil",
                                    "estado": "PE",
                                    "descricao": "Desenvolvimento de aplicações Java"
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idVaga").value(10))
                .andExpect(jsonPath("$.titulo").value(TITULO));

        verify(vagaService).atualizar(
                10L,
                CNPJ,
                TITULO,
                PAIS,
                ESTADO,
                DESCRICAO
        );
    }

    @Test
    void deveExcluirVagaERetornar204() throws Exception {

        mockMvc.perform(
                        delete("/api/vagas/10")
                                .param("cnpjEmpresa", CNPJ)
                )
                .andExpect(status().isNoContent());

        verify(vagaService).excluir(10L, CNPJ);
    }

    @Test
    void deveRetornar404QuandoVagaNaoExistir() throws Exception {

        when(vagaService.buscarPorId(99L))
                .thenThrow(new EntityNotFoundException("Vaga não encontrada."));

        mockMvc.perform(get("/api/vagas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Vaga não encontrada."));
    }

    private Vaga criarVagaMock(Long id) {
        Vaga vaga = mock(Vaga.class);

        when(vaga.getIdVaga()).thenReturn(id);
        when(vaga.getTitulo()).thenReturn(TITULO);
        when(vaga.getPais()).thenReturn(PAIS);
        when(vaga.getEstado()).thenReturn(ESTADO);
        when(vaga.getDescricao()).thenReturn(DESCRICAO);

        return vaga;
    }
}