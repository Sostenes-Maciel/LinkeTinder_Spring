package com.zg.acelera.mscompetencias.service;

import com.zg.acelera.mscompetencias.dto.CompetenciaResponse;
import com.zg.acelera.mscompetencias.model.EmpresaCompetencia;
import com.zg.acelera.mscompetencias.model.Competencia;
import com.zg.acelera.mscompetencias.model.id.EmpresaCompetenciaId;
import com.zg.acelera.mscompetencias.repository.EmpresaCompetenciaRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class EmpresaCompetenciaService {

    private final EmpresaCompetenciaRepository associacaoRepository;
    private final CompetenciaService competenciaService;

    public EmpresaCompetenciaService(
            EmpresaCompetenciaRepository associacaoRepository,
            CompetenciaService competenciaService
    ) {
        this.associacaoRepository = associacaoRepository;
        this.competenciaService = competenciaService;
    }

    @Transactional
    public List<CompetenciaResponse> associar(
            String cnpj,
            List<String> nomesCompetencias
    ) {
        String cnpjNormalizado = validarCnpj(cnpj);

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

            EmpresaCompetenciaId id = new EmpresaCompetenciaId(
                    cnpjNormalizado,
                    competencia.getIdCompetencia()
            );

            if (!associacaoRepository.existsById(id)) {

                EmpresaCompetencia associacao =
                        new EmpresaCompetencia(
                                cnpjNormalizado,
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

    public List<CompetenciaResponse> listarPorEmpresa(String cnpj) {

        String cnpjNormalizado = validarCnpj(cnpj);

        return associacaoRepository
                .buscarPorCnpjEmpresa(cnpjNormalizado)
                .stream()
                .map(associacao ->
                        CompetenciaResponse.from(
                                associacao.getCompetencia()
                        )
                )
                .toList();
    }

    @Transactional
    public void removerAssociacao(String cnpj, Long idCompetencia) {

        String cnpjNormalizado = validarCnpj(cnpj);

        EmpresaCompetenciaId id = new EmpresaCompetenciaId(
                cnpjNormalizado,
                idCompetencia
        );

        if (!associacaoRepository.existsById(id)) {
            throw new EntityNotFoundException(
                    "Associação entre empresa e competência não encontrada."
            );
        }

        associacaoRepository.deleteById(id);
    }

    private String validarCnpj(String cnpj) {

        if (cnpj == null || cnpj.isBlank()) {
            throw new IllegalArgumentException(
                    "O CNPJ da empresa é obrigatório."
            );
        }

        String cnpjNormalizado = cnpj.replaceAll("\\D", "");

        if (!cnpjNormalizado.matches("\\d{14}")) {
            throw new IllegalArgumentException(
                    "O CNPJ deve possuir exatamente 14 dígitos."
            );
        }

        return cnpjNormalizado;
    }
}