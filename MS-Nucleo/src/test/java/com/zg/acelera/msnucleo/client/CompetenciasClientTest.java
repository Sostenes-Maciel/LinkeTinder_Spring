package com.zg.acelera.msnucleo.client;

import org.junit.jupiter.api.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CompetenciasClientTest {

    @Test
    void deveConsultarCompetenciasDeCandidatosEmLote() {
        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer servidor =
                MockRestServiceServer.bindTo(builder).build();

        CompetenciasClient client =
                new CompetenciasClient(builder, "http://localhost:8082");

        servidor.expect(
                        requestTo(
                                "http://localhost:8082/api/competencias/candidatos/consulta-lote"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                            "cpfs": [
                                "12345678900",
                                "98765432100"
                            ]
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                            "12345678900": [
                                {
                                    "idCompetencia": 1,
                                    "nome": "Java"
                                }
                            ],
                            "98765432100": []
                        }
                        """, MediaType.APPLICATION_JSON));

        Map<String, List<com.zg.acelera.msnucleo.dto.CompetenciaResponseDTO>>
                resultado = client.consultarPorCandidatosEmLote(
                List.of(
                        "123.456.789-00",
                        "12345678900",
                        "987.654.321-00"
                )
        );

        assertEquals(2, resultado.size());
        assertEquals(
                "Java",
                resultado.get("12345678900").get(0).nome()
        );
        assertTrue(resultado.get("98765432100").isEmpty());

        servidor.verify();
    }

    @Test
    void deveDividirConsultasComMaisDeQuinhentosCpfs() {
        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer servidor =
                MockRestServiceServer.bindTo(builder).build();

        CompetenciasClient client =
                new CompetenciasClient(builder, "http://localhost:8082");

        String rota =
                "http://localhost:8082/api/competencias/candidatos/consulta-lote";

        servidor.expect(requestTo(rota))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.cpfs.length()").value(500))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        servidor.expect(requestTo(rota))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.cpfs.length()").value(1))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        List<String> cpfs = IntStream.range(0, 501)
                .mapToObj(numero -> String.format("%011d", numero))
                .toList();

        Map<String, List<com.zg.acelera.msnucleo.dto.CompetenciaResponseDTO>>
                resultado = client.consultarPorCandidatosEmLote(cpfs);

        assertEquals(501, resultado.size());
        assertTrue(resultado.values().stream().allMatch(List::isEmpty));

        servidor.verify();
    }
}