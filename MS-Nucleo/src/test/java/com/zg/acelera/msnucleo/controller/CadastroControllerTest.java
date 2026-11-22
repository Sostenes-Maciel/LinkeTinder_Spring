package com.zg.acelera.msnucleo.controller;

import com.zg.acelera.msnucleo.service.CandidatoService;
import com.zg.acelera.msnucleo.service.EmpresaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        CandidatoController.class,
        EmpresaController.class
})
class CadastroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CandidatoService candidatoService;

    @MockitoBean
    private EmpresaService empresaService;

    @Test
    void deveCadastrarCandidatoERetornar201() throws Exception {

        mockMvc.perform(
                        post("/api/candidatos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "cpf": "12345678901",
                                    "nome": "Ana Souza",
                                    "email": "ana@email.com",
                                    "pais": "Brasil",
                                    "estado": "PE",
                                    "cep": "50000000",
                                    "descricao": "Desenvolvedora Java",
                                    "idade": 25
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.mensagem")
                        .value("Candidato cadastrado com sucesso."));

        verify(candidatoService).cadastrar(
                "12345678901",
                "Ana Souza",
                "ana@email.com",
                "Brasil",
                "PE",
                "50000000",
                "Desenvolvedora Java",
                25
        );
    }

    @Test
    void deveRetornar400QuandoCadastroDeCandidatoFalhar() throws Exception {

        when(candidatoService.cadastrar(
                "12345678901",
                "Ana Souza",
                "ana@email.com",
                "Brasil",
                "PE",
                "50000000",
                "Desenvolvedora Java",
                25
        )).thenThrow(new IllegalArgumentException("CPF já cadastrado."));

        mockMvc.perform(
                        post("/api/candidatos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "cpf": "12345678901",
                                    "nome": "Ana Souza",
                                    "email": "ana@email.com",
                                    "pais": "Brasil",
                                    "estado": "PE",
                                    "cep": "50000000",
                                    "descricao": "Desenvolvedora Java",
                                    "idade": 25
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("CPF já cadastrado."));
    }

    @Test
    void deveCadastrarEmpresaERetornar201() throws Exception {

        mockMvc.perform(
                        post("/api/empresas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "cnpj": "12345678000199",
                                    "nome": "Empresa Teste",
                                    "email": "contato@empresa.com",
                                    "pais": "Brasil",
                                    "estado": "PE",
                                    "cep": "50000000",
                                    "descricao": "Empresa de tecnologia"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.mensagem")
                        .value("Empresa cadastrada com sucesso."));

        verify(empresaService).cadastrar(
                "12345678000199",
                "Empresa Teste",
                "contato@empresa.com",
                "Brasil",
                "PE",
                "50000000",
                "Empresa de tecnologia"
        );
    }

    @Test
    void deveRetornar409QuandoCadastroDeEmpresaViolarIntegridade() throws Exception {

        when(empresaService.cadastrar(
                "12345678000199",
                "Empresa Teste",
                "contato@empresa.com",
                "Brasil",
                "PE",
                "50000000",
                "Empresa de tecnologia"
        )).thenThrow(new DataIntegrityViolationException("E-mail duplicado."));

        mockMvc.perform(
                        post("/api/empresas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "cnpj": "12345678000199",
                                    "nome": "Empresa Teste",
                                    "email": "contato@empresa.com",
                                    "pais": "Brasil",
                                    "estado": "PE",
                                    "cep": "50000000",
                                    "descricao": "Empresa de tecnologia"
                                }
                                """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem")
                        .value("Não foi possível realizar a operação devido a um conflito de dados."));
    }
}