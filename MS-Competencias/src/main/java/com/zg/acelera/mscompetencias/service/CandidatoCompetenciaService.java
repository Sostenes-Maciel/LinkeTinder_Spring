package com.zg.acelera.mscompetencias.service;

import com.zg.acelera.mscompetencias.dto.CompetenciaResponse;
import com.zg.acelera.mscompetencias.model.CandidatoCompetencia;
import com.zg.acelera.mscompetencias.model.Competencia;
import com.zg.acelera.mscompetencias.model.id.CandidatoCompetenciaId;
import com.zg.acelera.mscompetencias.repository.CandidatoCompetenciaRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class CandidatoCompetenciaService {

    private final CandidatoCompetenciaRepository associacaoRepository;
    private final CompetenciaService competenciaService;

    public CandidatoCompetenciaService(
            CandidatoCompetenciaRepository associacaoRepository,
            CompetenciaService competenciaService
    ) {
        this.associacaoRepository = associacaoRepository;
        this.competenciaService = competenciaService;
    }

    @Transactional
    public List<CompetenciaResponse> associar(
            String cpf,
            List<String> nomesCompetencias
    ) {
        String cpfNormalizado = validarCpf(cpf);

        if (nomesCompetencias == null || nomesCompetencias.isEmpty()) {
            throw new IllegalArgumentException(
                    "Informe pelo menos uma competência."
            );
        }

        Map<Long, Competencia> competenciasEncontradas = new LinkedHashMap<>();

        for (String nome : nomesCompetencias) {
            Competencia competencia =
                    competenciaService.buscarOuCriar(nome);

            competenciasEncontradas.put(
                    competencia.getIdCompetencia(),
                    competencia
            );
        }

        for (Competencia competencia : competenciasEncontradas.values()) {

            CandidatoCompetenciaId id = new CandidatoCompetenciaId(
                    cpfNormalizado,
                    competencia.getIdCompetencia()
            );

            if (!associacaoRepository.existsById(id)) {
                CandidatoCompetencia associacao =
                        new CandidatoCompetencia(
                                cpfNormalizado,
                                competencia
                        );

                associacaoRepository.save(associacao);
            }
        }

        return competenciasEncontradas.values()
                .stream()
                .map(CompetenciaResponse::from)
                .toList();
    }

    public List<CompetenciaResponse> listarPorCandidato(String cpf) {

        String cpfNormalizado = validarCpf(cpf);

        return associacaoRepository
                .buscarPorCpfCandidato(cpfNormalizado)
                .stream()
                .map(associacao ->
                        CompetenciaResponse.from(
                                associacao.getCompetencia()
                        )
                )
                .toList();
    }

    @Transactional
    public void removerAssociacao(String cpf, Long idCompetencia) {

        String cpfNormalizado = validarCpf(cpf);

        CandidatoCompetenciaId id = new CandidatoCompetenciaId(
                cpfNormalizado,
                idCompetencia
        );

        if (!associacaoRepository.existsById(id)) {
            throw new EntityNotFoundException(
                    "Associação entre candidato e competência não encontrada."
            );
        }

        associacaoRepository.deleteById(id);
    }

    private String validarCpf(String cpf) {

        if (cpf == null || cpf.isBlank()) {
            throw new IllegalArgumentException(
                    "O CPF do candidato é obrigatório."
            );
        }

        String cpfNormalizado = cpf.trim()
                .replaceAll("[.\\-\\s]", "");

        if (!cpfNormalizado.matches("\\d{11}")) {
            throw new IllegalArgumentException(
                    "O CPF deve possuir exatamente 11 dígitos."
            );
        }

        return cpfNormalizado;
    }
}