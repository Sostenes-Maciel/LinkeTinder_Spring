package com.zg.acelera.mscompetencias.service;

import com.zg.acelera.mscompetencias.dto.CompetenciaResponse;
import com.zg.acelera.mscompetencias.model.Competencia;
import com.zg.acelera.mscompetencias.model.VagaCompetencia;
import com.zg.acelera.mscompetencias.model.id.VagaCompetenciaId;
import com.zg.acelera.mscompetencias.repository.VagaCompetenciaRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class VagaCompetenciaService {

    private final VagaCompetenciaRepository associacaoRepository;
    private final CompetenciaService competenciaService;

    public VagaCompetenciaService(
            VagaCompetenciaRepository associacaoRepository,
            CompetenciaService competenciaService
    ) {
        this.associacaoRepository = associacaoRepository;
        this.competenciaService = competenciaService;
    }

    @Transactional
    public List<CompetenciaResponse> associar(
            Long idVaga,
            List<String> nomesCompetencias
    ) {
        validarIdVaga(idVaga);

        if (nomesCompetencias == null || nomesCompetencias.isEmpty()) {
            throw new IllegalArgumentException(
                    "Informe pelo menos uma competência."
            );
        }

        Map<Long, Competencia> competenciasEncontradas =
                new LinkedHashMap<>();

        for (String nome : nomesCompetencias) {

            Competencia competencia =
                    competenciaService.buscarOuCriar(nome);

            competenciasEncontradas.put(
                    competencia.getIdCompetencia(),
                    competencia
            );
        }

        for (Competencia competencia : competenciasEncontradas.values()) {

            VagaCompetenciaId id = new VagaCompetenciaId(
                    idVaga,
                    competencia.getIdCompetencia()
            );

            if (!associacaoRepository.existsById(id)) {

                VagaCompetencia associacao =
                        new VagaCompetencia(idVaga, competencia);

                associacaoRepository.save(associacao);
            }
        }

        return competenciasEncontradas.values()
                .stream()
                .map(CompetenciaResponse::from)
                .toList();
    }

    public List<CompetenciaResponse> listarPorVaga(Long idVaga) {

        validarIdVaga(idVaga);

        return associacaoRepository
                .buscarPorIdVaga(idVaga)
                .stream()
                .map(associacao ->
                        CompetenciaResponse.from(
                                associacao.getCompetencia()
                        )
                )
                .toList();
    }

    @Transactional
    public void removerAssociacao(Long idVaga, Long idCompetencia) {

        validarIdVaga(idVaga);

        if (idCompetencia == null || idCompetencia <= 0) {
            throw new IllegalArgumentException(
                    "O identificador da competência é inválido."
            );
        }

        VagaCompetenciaId id = new VagaCompetenciaId(
                idVaga,
                idCompetencia
        );

        if (!associacaoRepository.existsById(id)) {
            throw new EntityNotFoundException(
                    "Associação entre vaga e competência não encontrada."
            );
        }

        associacaoRepository.deleteById(id);
    }

    private void validarIdVaga(Long idVaga) {

        if (idVaga == null || idVaga <= 0) {
            throw new IllegalArgumentException(
                    "O identificador da vaga é inválido."
            );
        }
    }
}