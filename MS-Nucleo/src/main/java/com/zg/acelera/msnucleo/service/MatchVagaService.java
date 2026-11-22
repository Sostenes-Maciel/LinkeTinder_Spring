package com.zg.acelera.msnucleo.service;

import com.zg.acelera.msnucleo.model.Candidato;
import com.zg.acelera.msnucleo.model.MatchVaga;
import com.zg.acelera.msnucleo.model.Vaga;
import com.zg.acelera.msnucleo.repository.CandidatoRepository;
import com.zg.acelera.msnucleo.repository.CurtidaRepository;
import com.zg.acelera.msnucleo.repository.MatchVagaRepository;
import com.zg.acelera.msnucleo.repository.VagaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class MatchVagaService {

    private final MatchVagaRepository matchVagaRepository;
    private final CurtidaRepository curtidaRepository;
    private final CandidatoRepository candidatoRepository;
    private final VagaRepository vagaRepository;

    public MatchVagaService(
            MatchVagaRepository matchVagaRepository,
            CurtidaRepository curtidaRepository,
            CandidatoRepository candidatoRepository,
            VagaRepository vagaRepository
    ) {
        this.matchVagaRepository = matchVagaRepository;
        this.curtidaRepository = curtidaRepository;
        this.candidatoRepository = candidatoRepository;
        this.vagaRepository = vagaRepository;
    }

    /**
     * Verifica se existem curtidas recíprocas para candidato e vaga.
     * Cria o Match somente quando as duas curtidas existem.
     */
    @Transactional
    public Optional<MatchVaga> processarPossivelMatch(
            String cpf,
            Long idVaga
    ) {
        String cpfNormalizado = normalizarCpf(cpf);
        validarIdVaga(idVaga);

        Candidato candidato = candidatoRepository.findById(cpfNormalizado)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Candidato não encontrado: " + cpfNormalizado
                        )
                );

        Vaga vaga = vagaRepository.findById(idVaga)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Vaga não encontrada: " + idVaga
                        )
                );

        String cnpjEmpresa = vaga.getEmpresa().getCnpj();

        boolean candidatoCurtiu =
                curtidaRepository
                        .existsByCandidato_CpfAndVaga_IdVagaAndEmpresaIsNull(
                                cpfNormalizado,
                                idVaga
                        );

        boolean empresaCurtiu =
                curtidaRepository
                        .existsByCandidato_CpfAndEmpresa_CnpjAndVaga_IdVaga(
                                cpfNormalizado,
                                cnpjEmpresa,
                                idVaga
                        );

        if (!candidatoCurtiu || !empresaCurtiu) {
            return Optional.empty();
        }

        Optional<MatchVaga> matchExistente =
                matchVagaRepository
                        .findByCandidato_CpfAndVaga_Empresa_CnpjAndVaga_IdVaga(
                                cpfNormalizado,
                                cnpjEmpresa,
                                idVaga
                        );

        if (matchExistente.isPresent()) {
            return matchExistente;
        }

        MatchVaga novoMatch = new MatchVaga(candidato, vaga);

        return Optional.of(matchVagaRepository.save(novoMatch));
    }

    public List<MatchVaga> listarPorCandidato(String cpf) {
        String cpfNormalizado = normalizarCpf(cpf);

        return matchVagaRepository.findByCandidato_Cpf(cpfNormalizado);
    }

    public List<MatchVaga> listarPorEmpresa(String cnpj) {
        String cnpjNormalizado = normalizarCnpj(cnpj);

        return matchVagaRepository.findByVaga_Empresa_Cnpj(cnpjNormalizado);
    }

    public Optional<MatchVaga> buscarPorTupla(
            String cpf,
            String cnpj,
            Long idVaga
    ) {
        String cpfNormalizado = normalizarCpf(cpf);
        String cnpjNormalizado = normalizarCnpj(cnpj);
        validarIdVaga(idVaga);

        return matchVagaRepository
                .findByCandidato_CpfAndVaga_Empresa_CnpjAndVaga_IdVaga(
                        cpfNormalizado,
                        cnpjNormalizado,
                        idVaga
                );
    }

    public boolean existeMatch(
            String cpf,
            String cnpj,
            Long idVaga
    ) {
        String cpfNormalizado = normalizarCpf(cpf);
        String cnpjNormalizado = normalizarCnpj(cnpj);
        validarIdVaga(idVaga);

        return matchVagaRepository
                .existsByCandidato_CpfAndVaga_Empresa_CnpjAndVaga_IdVaga(
                        cpfNormalizado,
                        cnpjNormalizado,
                        idVaga
                );
    }

    private String normalizarCpf(String cpf) {
        if (cpf == null) {
            throw new IllegalArgumentException("CPF não informado.");
        }

        String normalizado = cpf.replaceAll("\\D", "");

        if (!normalizado.matches("\\d{11}")) {
            throw new IllegalArgumentException(
                    "O CPF deve possuir exatamente 11 dígitos."
            );
        }

        return normalizado;
    }

    private String normalizarCnpj(String cnpj) {
        if (cnpj == null) {
            throw new IllegalArgumentException("CNPJ não informado.");
        }

        String normalizado = cnpj.replaceAll("\\D", "");

        if (!normalizado.matches("\\d{14}")) {
            throw new IllegalArgumentException(
                    "O CNPJ deve possuir exatamente 14 dígitos."
            );
        }

        return normalizado;
    }

    private void validarIdVaga(Long idVaga) {
        if (idVaga == null || idVaga <= 0) {
            throw new IllegalArgumentException(
                    "Identificador da vaga inválido."
            );
        }
    }
}