package com.zg.acelera.msnucleo.service;

import com.zg.acelera.msnucleo.client.CompetenciasClient;
import com.zg.acelera.msnucleo.model.Candidato;
import com.zg.acelera.msnucleo.repository.CandidatoRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CandidatoService {

    private final CandidatoRepository candidatoRepository;
    private final CompetenciasClient competenciasClient;

    public CandidatoService(
            CandidatoRepository candidatoRepository,
            CompetenciasClient competenciasClient
    ) {
        this.candidatoRepository = candidatoRepository;
        this.competenciasClient = competenciasClient;
    }

    public List<Candidato> listarTodos() {
        return candidatoRepository.findAll();
    }

    public Candidato buscarPorCpf(String cpf) {
        String cpfNormalizado = normalizarCpf(cpf);

        return candidatoRepository.findById(cpfNormalizado)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Candidato não encontrado: " + cpfNormalizado
                        )
                );
    }

    @Transactional
    public Candidato cadastrar(
            String cpf,
            String nome,
            String email,
            String pais,
            String estado,
            String cep,
            String descricao,
            Integer idade
    ) {
        String cpfNormalizado = normalizarCpf(cpf);
        String nomeNormalizado = validarObrigatorio(nome, "Nome");
        String emailNormalizado = validarObrigatorio(email, "E-mail");

        if (candidatoRepository.existsById(cpfNormalizado)) {
            throw new IllegalArgumentException(
                    "Já existe um candidato cadastrado com este CPF."
            );
        }

        if (candidatoRepository.existsByEmail(emailNormalizado)) {
            throw new IllegalArgumentException(
                    "Já existe um candidato cadastrado com este e-mail."
            );
        }

        Candidato candidato = new Candidato(
                cpfNormalizado,
                nomeNormalizado,
                emailNormalizado,
                normalizarOpcional(pais),
                normalizarOpcional(estado),
                normalizarOpcional(cep),
                normalizarOpcional(descricao),
                idade
        );

        return candidatoRepository.save(candidato);
    }

    @Transactional
    public Candidato atualizar(
            String cpf,
            String nome,
            String email,
            String pais,
            String estado,
            String cep,
            String descricao,
            Integer idade
    ) {
        String cpfNormalizado = normalizarCpf(cpf);
        String nomeNormalizado = validarObrigatorio(nome, "Nome");
        String emailNormalizado = validarObrigatorio(email, "E-mail");

        Candidato candidato = buscarPorCpf(cpfNormalizado);

        candidatoRepository.findByEmail(emailNormalizado)
                .filter(outro -> !outro.getCpf().equals(cpfNormalizado))
                .ifPresent(outro -> {
                    throw new IllegalArgumentException(
                            "O e-mail informado já pertence a outro candidato."
                    );
                });

        candidato.atualizarDados(
                nomeNormalizado,
                emailNormalizado,
                normalizarOpcional(pais),
                normalizarOpcional(estado),
                normalizarOpcional(cep),
                normalizarOpcional(descricao),
                idade
        );

        return candidatoRepository.save(candidato);
    }

    @Transactional
    public void excluir(String cpf) {
        Candidato candidato = buscarPorCpf(cpf);

        competenciasClient.removerTodasDoCandidato(candidato.getCpf());

        candidatoRepository.delete(candidato);
    }

    private String normalizarCpf(String cpf) {
        if (cpf == null) {
            throw new IllegalArgumentException("CPF não informado.");
        }

        String cpfNormalizado = cpf.replaceAll("\\D", "");

        if (!cpfNormalizado.matches("\\d{11}")) {
            throw new IllegalArgumentException(
                    "O CPF deve possuir exatamente 11 dígitos."
            );
        }

        return cpfNormalizado;
    }

    private String validarObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    campo + " é obrigatório."
            );
        }

        return valor.trim();
    }

    private String normalizarOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}