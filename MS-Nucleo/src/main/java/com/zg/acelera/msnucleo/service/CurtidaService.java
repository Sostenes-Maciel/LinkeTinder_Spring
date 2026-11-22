package com.zg.acelera.msnucleo.service;

import com.zg.acelera.msnucleo.model.Candidato;
import com.zg.acelera.msnucleo.model.Curtida;
import com.zg.acelera.msnucleo.model.Empresa;
import com.zg.acelera.msnucleo.model.Vaga;
import com.zg.acelera.msnucleo.repository.CandidatoRepository;
import com.zg.acelera.msnucleo.repository.CurtidaRepository;
import com.zg.acelera.msnucleo.repository.EmpresaRepository;
import com.zg.acelera.msnucleo.repository.VagaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CurtidaService {

    private final CurtidaRepository curtidaRepository;
    private final CandidatoRepository candidatoRepository;
    private final EmpresaRepository empresaRepository;
    private final VagaRepository vagaRepository;
    private final MatchVagaService matchVagaService;

    public CurtidaService(
            CurtidaRepository curtidaRepository,
            CandidatoRepository candidatoRepository,
            EmpresaRepository empresaRepository,
            VagaRepository vagaRepository,
            MatchVagaService matchVagaService
    ) {
        this.curtidaRepository = curtidaRepository;
        this.candidatoRepository = candidatoRepository;
        this.empresaRepository = empresaRepository;
        this.vagaRepository = vagaRepository;
        this.matchVagaService = matchVagaService;
    }

    /**
     * Registra uma curtida de candidato em uma vaga.
     */
    @Transactional
    public Curtida curtirVaga(String cpf, Long idVaga) {
        String cpfNormalizado = normalizarCpf(cpf);
        validarIdVaga(idVaga);

        Candidato candidato = buscarCandidato(cpfNormalizado);
        Vaga vaga = buscarVaga(idVaga);

        boolean jaCurtiu = curtidaRepository
                .existsByCandidato_CpfAndVaga_IdVagaAndEmpresaIsNull(
                        cpfNormalizado,
                        idVaga
                );

        if (jaCurtiu) {
            throw new IllegalArgumentException(
                    "O candidato já curtiu esta vaga."
            );
        }

        Curtida curtida = new Curtida(candidato, vaga);

        Curtida curtidaSalva = curtidaRepository.save(curtida);

        // Verifica se a empresa já havia curtido o candidato.
        matchVagaService.processarPossivelMatch(
                cpfNormalizado,
                idVaga
        );

        return curtidaSalva;
    }

    /**
     * Registra uma curtida de empresa em candidato para uma vaga específica.
     */
    @Transactional
    public Curtida curtirCandidato(
            String cnpj,
            String cpf,
            Long idVaga
    ) {
        String cnpjNormalizado = normalizarCnpj(cnpj);
        String cpfNormalizado = normalizarCpf(cpf);

        validarIdVaga(idVaga);

        Empresa empresa = buscarEmpresa(cnpjNormalizado);
        Candidato candidato = buscarCandidato(cpfNormalizado);

        // Garante que a vaga pertence à empresa que está curtindo.
        Vaga vaga = vagaRepository
                .findByIdVagaAndCnpjEmpresa(idVaga, cnpjNormalizado)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "A vaga informada não pertence à empresa."
                        )
                );

        boolean jaCurtiu = curtidaRepository
                .existsByCandidato_CpfAndEmpresa_CnpjAndVaga_IdVaga(
                        cpfNormalizado,
                        cnpjNormalizado,
                        idVaga
                );

        if (jaCurtiu) {
            throw new IllegalArgumentException(
                    "A empresa já curtiu este candidato para esta vaga."
            );
        }

        Curtida curtida = new Curtida(
                candidato,
                vaga,
                empresa
        );

        Curtida curtidaSalva = curtidaRepository.save(curtida);

        // Verifica se o candidato já havia curtido a mesma vaga.
        matchVagaService.processarPossivelMatch(
                cpfNormalizado,
                idVaga
        );

        return curtidaSalva;
    }

    /**
     * Lista todas as curtidas de um candidato.
     */
    public List<Curtida> listarPorCandidato(String cpf) {
        String cpfNormalizado = normalizarCpf(cpf);

        return curtidaRepository.findByCandidato_Cpf(cpfNormalizado);
    }

    /**
     * Lista todas as curtidas recebidas por uma vaga.
     */
    public List<Curtida> listarPorVaga(Long idVaga) {
        validarIdVaga(idVaga);
        buscarVaga(idVaga);

        return curtidaRepository.findByVaga_IdVaga(idVaga);
    }

    /**
     * Lista as curtidas realizadas por uma empresa.
     */
    public List<Curtida> listarPorEmpresa(String cnpj) {
        String cnpjNormalizado = normalizarCnpj(cnpj);

        buscarEmpresa(cnpjNormalizado);

        return curtidaRepository.findByEmpresa_Cnpj(cnpjNormalizado);
    }

    /**
     * Remove a curtida realizada pelo candidato.
     * A remoção é bloqueada se já existir Match para a tupla.
     */
    @Transactional
    public void removerCurtidaCandidato(String cpf, Long idVaga) {
        String cpfNormalizado = normalizarCpf(cpf);
        validarIdVaga(idVaga);

        Curtida curtida = curtidaRepository
                .findByCandidato_CpfAndVaga_IdVagaAndEmpresaIsNull(
                        cpfNormalizado,
                        idVaga
                )
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Curtida do candidato não encontrada."
                        )
                );

        String cnpjEmpresa = curtida.getVaga()
                .getEmpresa()
                .getCnpj();

        verificarAusenciaDeMatch(
                cpfNormalizado,
                cnpjEmpresa,
                idVaga
        );

        curtidaRepository.delete(curtida);
    }

    /**
     * Remove a curtida realizada pela empresa.
     * A remoção é bloqueada se já existir Match para a tupla.
     */
    @Transactional
    public void removerCurtidaEmpresa(
            String cnpj,
            String cpf,
            Long idVaga
    ) {
        String cnpjNormalizado = normalizarCnpj(cnpj);
        String cpfNormalizado = normalizarCpf(cpf);

        validarIdVaga(idVaga);

        Curtida curtida = curtidaRepository
                .findByCandidato_CpfAndEmpresa_CnpjAndVaga_IdVaga(
                        cpfNormalizado,
                        cnpjNormalizado,
                        idVaga
                )
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Curtida da empresa não encontrada."
                        )
                );

        verificarAusenciaDeMatch(
                cpfNormalizado,
                cnpjNormalizado,
                idVaga
        );

        curtidaRepository.delete(curtida);
    }

    private void verificarAusenciaDeMatch(
            String cpf,
            String cnpj,
            Long idVaga
    ) {
        if (matchVagaService.existeMatch(cpf, cnpj, idVaga)) {
            throw new IllegalStateException(
                    "A curtida não pode ser removida porque já existe "
                            + "um Match para este candidato, empresa e vaga."
            );
        }
    }

    private Candidato buscarCandidato(String cpf) {
        return candidatoRepository.findById(cpf)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Candidato não encontrado: " + cpf
                        )
                );
    }

    private Empresa buscarEmpresa(String cnpj) {
        return empresaRepository.findById(cnpj)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Empresa não encontrada: " + cnpj
                        )
                );
    }

    private Vaga buscarVaga(Long idVaga) {
        return vagaRepository.findById(idVaga)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Vaga não encontrada: " + idVaga
                        )
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