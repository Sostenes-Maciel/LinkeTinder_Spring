package com.zg.acelera.msnucleo.service;

import com.zg.acelera.msnucleo.model.Empresa;
import com.zg.acelera.msnucleo.repository.EmpresaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public List<Empresa> listarTodas() {
        return empresaRepository.findAll();
    }

    public Empresa buscarPorCnpj(String cnpj) {
        String cnpjNormalizado = normalizarCnpj(cnpj);

        return empresaRepository.findById(cnpjNormalizado)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Empresa não encontrada: " + cnpjNormalizado
                        )
                );
    }

    @Transactional
    public Empresa cadastrar(
            String cnpj,
            String nome,
            String email,
            String pais,
            String estado,
            String cep,
            String descricao
    ) {
        String cnpjNormalizado = normalizarCnpj(cnpj);
        String nomeNormalizado = validarObrigatorio(nome, "Nome");
        String emailNormalizado = validarObrigatorio(email, "E-mail");

        if (empresaRepository.existsById(cnpjNormalizado)) {
            throw new IllegalArgumentException(
                    "Já existe uma empresa cadastrada com este CNPJ."
            );
        }

        if (empresaRepository.existsByEmail(emailNormalizado)) {
            throw new IllegalArgumentException(
                    "Já existe uma empresa cadastrada com este e-mail."
            );
        }

        Empresa empresa = new Empresa(
                cnpjNormalizado,
                nomeNormalizado,
                emailNormalizado,
                normalizarOpcional(pais),
                normalizarOpcional(estado),
                normalizarOpcional(cep),
                normalizarOpcional(descricao)
        );

        return empresaRepository.save(empresa);
    }

    @Transactional
    public Empresa atualizar(
            String cnpj,
            String nome,
            String email,
            String pais,
            String estado,
            String cep,
            String descricao
    ) {
        String cnpjNormalizado = normalizarCnpj(cnpj);
        String nomeNormalizado = validarObrigatorio(nome, "Nome");
        String emailNormalizado = validarObrigatorio(email, "E-mail");

        Empresa empresa = buscarPorCnpj(cnpjNormalizado);

        empresaRepository.findByEmail(emailNormalizado)
                .filter(outro -> !outro.getCnpj().equals(cnpjNormalizado))
                .ifPresent(outro -> {
                    throw new IllegalArgumentException(
                            "O e-mail informado já pertence a outra empresa."
                    );
                });

        empresa.atualizarDados(
                nomeNormalizado,
                emailNormalizado,
                normalizarOpcional(pais),
                normalizarOpcional(estado),
                normalizarOpcional(cep),
                normalizarOpcional(descricao)
        );

        return empresaRepository.save(empresa);
    }

    @Transactional
    public void excluir(String cnpj) {
        Empresa empresa = buscarPorCnpj(cnpj);
        empresaRepository.delete(empresa);
    }

    private String normalizarCnpj(String cnpj) {
        if (cnpj == null) {
            throw new IllegalArgumentException("CNPJ não informado.");
        }

        String cnpjNormalizado = cnpj.replaceAll("\\D", "");

        if (!cnpjNormalizado.matches("\\d{14}")) {
            throw new IllegalArgumentException(
                    "O CNPJ deve possuir exatamente 14 dígitos."
            );
        }

        return cnpjNormalizado;
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