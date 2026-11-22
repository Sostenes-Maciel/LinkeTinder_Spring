package com.zg.acelera.msnucleo.service;

import com.zg.acelera.msnucleo.model.Empresa;
import com.zg.acelera.msnucleo.model.Vaga;
import com.zg.acelera.msnucleo.repository.EmpresaRepository;
import com.zg.acelera.msnucleo.repository.VagaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class VagaService {

    private final VagaRepository vagaRepository;
    private final EmpresaRepository empresaRepository;

    public VagaService(
            VagaRepository vagaRepository,
            EmpresaRepository empresaRepository
    ) {
        this.vagaRepository = vagaRepository;
        this.empresaRepository = empresaRepository;
    }

    public List<Vaga> listarTodas() {
        return vagaRepository.findAll();
    }

    public List<Vaga> listarPorEmpresa(String cnpj) {
        String cnpjNormalizado = normalizarCnpj(cnpj);

        verificarEmpresaExiste(cnpjNormalizado);

        return vagaRepository.findByCnpjEmpresa(cnpjNormalizado);
    }

    public Vaga buscarPorId(Long idVaga) {
        validarIdVaga(idVaga);

        return vagaRepository.findById(idVaga)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Vaga não encontrada: " + idVaga
                        )
                );
    }

    public Vaga buscarPorIdEEmpresa(Long idVaga, String cnpjEmpresa) {
        validarIdVaga(idVaga);

        String cnpjNormalizado = normalizarCnpj(cnpjEmpresa);

        return vagaRepository
                .findByIdVagaAndCnpjEmpresa(idVaga, cnpjNormalizado)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Vaga não encontrada para a empresa informada."
                        )
                );
    }

    @Transactional
    public Vaga cadastrar(
            String titulo,
            String pais,
            String estado,
            String descricao,
            String cnpjEmpresa
    ) {
        String tituloNormalizado = validarObrigatorio(titulo, "Título");
        String cnpjNormalizado = normalizarCnpj(cnpjEmpresa);

        Empresa empresa = verificarEmpresaExiste(cnpjNormalizado);

        Vaga vaga = new Vaga(
                tituloNormalizado,
                normalizarOpcional(pais),
                normalizarOpcional(estado),
                normalizarOpcional(descricao),
                empresa
        );

        return vagaRepository.save(vaga);
    }

    @Transactional
    public Vaga atualizar(
            Long idVaga,
            String titulo,
            String pais,
            String estado,
            String descricao,
            String cnpjEmpresa
    ) {
        String tituloNormalizado = validarObrigatorio(titulo, "Título");

        Vaga vaga = buscarPorIdEEmpresa(idVaga, cnpjEmpresa);

        vaga.atualizarDados(
                tituloNormalizado,
                normalizarOpcional(pais),
                normalizarOpcional(estado),
                normalizarOpcional(descricao)
        );

        return vagaRepository.save(vaga);
    }

    @Transactional
    public void excluir(Long idVaga, String cnpjEmpresa) {
        Vaga vaga = buscarPorIdEEmpresa(idVaga, cnpjEmpresa);
        vagaRepository.delete(vaga);
    }

    private Empresa verificarEmpresaExiste(String cnpj) {
        return empresaRepository.findById(cnpj)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Empresa não encontrada: " + cnpj
                        )
                );
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

    private void validarIdVaga(Long idVaga) {
        if (idVaga == null || idVaga <= 0) {
            throw new IllegalArgumentException(
                    "Identificador da vaga inválido."
            );
        }
    }
}