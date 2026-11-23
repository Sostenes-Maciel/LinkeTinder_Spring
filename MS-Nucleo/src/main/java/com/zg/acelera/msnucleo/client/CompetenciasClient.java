package com.zg.acelera.msnucleo.client;

import com.zg.acelera.msnucleo.dto.AssociarCompetenciasRequestDTO;
import com.zg.acelera.msnucleo.dto.CompetenciaRequestDTO;
import com.zg.acelera.msnucleo.dto.CompetenciaResponseDTO;
import com.zg.acelera.msnucleo.dto.ConsultaCompetenciasLoteRequestDTO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CompetenciasClient {

    private static final int TAMANHO_MAXIMO_LOTE_CPFS = 500;

    private final RestClient restClient;

    public CompetenciasClient(
            RestClient.Builder builder,
            @Value("${integracao.ms-competencias.url}") String url
    ) {
        this.restClient = builder
                .baseUrl(url)
                .build();
    }

    public List<CompetenciaResponseDTO> listarCompetencias() {
        List<CompetenciaResponseDTO> resposta = restClient.get()
                .uri("/api/competencias")
                .retrieve()
                .body(new ParameterizedTypeReference<
                        List<CompetenciaResponseDTO>
                        >() {
                });

        return resposta == null ? List.of() : resposta;
    }

    public CompetenciaResponseDTO buscarCompetencia(Long id) {
        return restClient.get()
                .uri("/api/competencias/{id}", id)
                .retrieve()
                .body(CompetenciaResponseDTO.class);
    }

    public CompetenciaResponseDTO cadastrarCompetencia(String nome) {
        return restClient.post()
                .uri("/api/competencias")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CompetenciaRequestDTO(nome))
                .retrieve()
                .body(CompetenciaResponseDTO.class);
    }

    public CompetenciaResponseDTO atualizarCompetencia(
            Long id,
            String nome
    ) {
        return restClient.put()
                .uri("/api/competencias/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CompetenciaRequestDTO(nome))
                .retrieve()
                .body(CompetenciaResponseDTO.class);
    }

    public void excluirCompetencia(Long id) {
        restClient.delete()
                .uri("/api/competencias/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }

    public List<CompetenciaResponseDTO> listarPorCandidato(String cpf) {
        return obterLista(
                "/api/candidatos/{cpf}/competencias",
                cpf
        );
    }

    public List<CompetenciaResponseDTO> associarAoCandidato(
            String cpf,
            List<String> competencias
    ) {
        return restClient.post()
                .uri("/api/candidatos/{cpf}/competencias", cpf)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AssociarCompetenciasRequestDTO(competencias))
                .retrieve()
                .body(new ParameterizedTypeReference<
                        List<CompetenciaResponseDTO>
                        >() {
                });
    }

    public void removerDoCandidato(String cpf, Long idCompetencia) {
        restClient.delete()
                .uri(
                        "/api/candidatos/{cpf}/competencias/{idCompetencia}",
                        cpf,
                        idCompetencia
                )
                .retrieve()
                .toBodilessEntity();
    }

    public void removerTodasDoCandidato(String cpf) {
        restClient.delete()
                .uri("/api/candidatos/{cpf}/competencias", cpf)
                .retrieve()
                .toBodilessEntity();
    }

    public List<CompetenciaResponseDTO> listarPorEmpresa(String cnpj) {
        return obterLista(
                "/api/empresas/{cnpj}/competencias",
                cnpj
        );
    }

    public List<CompetenciaResponseDTO> associarAoEmpresa(
            String cnpj,
            List<String> competencias
    ) {
        return restClient.post()
                .uri("/api/empresas/{cnpj}/competencias", cnpj)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AssociarCompetenciasRequestDTO(competencias))
                .retrieve()
                .body(new ParameterizedTypeReference<
                        List<CompetenciaResponseDTO>
                        >() {
                });
    }

    public void removerDaEmpresa(String cnpj, Long idCompetencia) {
        restClient.delete()
                .uri(
                        "/api/empresas/{cnpj}/competencias/{idCompetencia}",
                        cnpj,
                        idCompetencia
                )
                .retrieve()
                .toBodilessEntity();
    }

    public void removerTodasDaEmpresa(String cnpj) {
        restClient.delete()
                .uri("/api/empresas/{cnpj}/competencias", cnpj)
                .retrieve()
                .toBodilessEntity();
    }

    public List<CompetenciaResponseDTO> listarPorVaga(Long idVaga) {
        return obterLista(
                "/api/vagas/{idVaga}/competencias",
                idVaga
        );
    }

    public List<CompetenciaResponseDTO> associarAVaga(
            Long idVaga,
            List<String> competencias
    ) {
        return restClient.post()
                .uri("/api/vagas/{idVaga}/competencias", idVaga)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AssociarCompetenciasRequestDTO(competencias))
                .retrieve()
                .body(new ParameterizedTypeReference<
                        List<CompetenciaResponseDTO>
                        >() {
                });
    }

    public void removerDaVaga(Long idVaga, Long idCompetencia) {
        restClient.delete()
                .uri(
                        "/api/vagas/{idVaga}/competencias/{idCompetencia}",
                        idVaga,
                        idCompetencia
                )
                .retrieve()
                .toBodilessEntity();
    }

    public Map<String, List<CompetenciaResponseDTO>>
    consultarPorCandidatosEmLote(List<String> cpfs) {

        if (cpfs == null) {
            throw new IllegalArgumentException(
                    "A lista de CPFs deve ser informada."
            );
        }

        List<String> cpfsNormalizados = cpfs.stream()
                .map(this::normalizarCpf)
                .distinct()
                .toList();

        Map<String, List<CompetenciaResponseDTO>> resultado =
                new LinkedHashMap<>();

        cpfsNormalizados.forEach(cpf ->
                resultado.put(cpf, new ArrayList<>())
        );

        for (
                int inicio = 0;
                inicio < cpfsNormalizados.size();
                inicio += TAMANHO_MAXIMO_LOTE_CPFS
        ) {
            int fim = Math.min(
                    inicio + TAMANHO_MAXIMO_LOTE_CPFS,
                    cpfsNormalizados.size()
            );

            List<String> lote = cpfsNormalizados.subList(inicio, fim);

            Map<String, List<CompetenciaResponseDTO>> resposta =
                    restClient.post()
                            .uri("/api/competencias/candidatos/consulta-lote")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(
                                    new ConsultaCompetenciasLoteRequestDTO(lote)
                            )
                            .retrieve()
                            .body(new ParameterizedTypeReference<
                                    Map<String, List<CompetenciaResponseDTO>>
                                    >() {
                            });

            if (resposta != null) {
                for (String cpf : lote) {
                    List<CompetenciaResponseDTO> competencias =
                            resposta.get(cpf);

                    resultado.put(
                            cpf,
                            competencias == null
                                    ? new ArrayList<>()
                                    : new ArrayList<>(competencias)
                    );
                }
            }
        }

        return resultado;
    }

    private List<CompetenciaResponseDTO> obterLista(
            String rota,
            Object identificador
    ) {
        List<CompetenciaResponseDTO> resposta = restClient.get()
                .uri(rota, identificador)
                .retrieve()
                .body(new ParameterizedTypeReference<
                        List<CompetenciaResponseDTO>
                        >() {
                });

        return resposta == null ? List.of() : resposta;
    }

    private String normalizarCpf(String cpf) {
        if (cpf == null) {
            throw new IllegalArgumentException(
                    "A lista não pode conter CPFs nulos."
            );
        }

        String cpfNormalizado = cpf.replaceAll("\\D", "");

        if (!cpfNormalizado.matches("\\d{11}")) {
            throw new IllegalArgumentException(
                    "Cada CPF deve possuir exatamente 11 dígitos."
            );
        }

        return cpfNormalizado;
    }
}