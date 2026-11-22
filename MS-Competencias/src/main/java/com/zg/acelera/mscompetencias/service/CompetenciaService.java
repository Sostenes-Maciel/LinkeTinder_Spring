package com.zg.acelera.mscompetencias.service;

import com.zg.acelera.mscompetencias.model.Competencia;
import com.zg.acelera.mscompetencias.repository.CompetenciaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CompetenciaService {

    private final CompetenciaRepository competenciaRepository;

    public CompetenciaService(CompetenciaRepository competenciaRepository) {
        this.competenciaRepository = competenciaRepository;
    }

    public List<Competencia> listarTodas() {
        return competenciaRepository.findAll();
    }

    public Competencia buscarPorId(Long id) {
        return competenciaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Competência não encontrada: " + id
                ));
    }

    @Transactional
    public Competencia criar(String nome) {
        String nomeNormalizado = validarNome(nome);

        if (competenciaRepository.findByNome(nomeNormalizado).isPresent()) {
            throw new IllegalArgumentException(
                    "Já existe uma competência com esse nome."
            );
        }

        return competenciaRepository.save(
                new Competencia(nomeNormalizado)
        );
    }

    @Transactional
    public Competencia buscarOuCriar(String nome) {
        String nomeNormalizado = validarNome(nome);

        Optional<Competencia> competenciaExistente =
                competenciaRepository.findByNome(nomeNormalizado);

        return competenciaExistente.orElseGet(() ->
                competenciaRepository.save(
                        new Competencia(nomeNormalizado)
                )
        );
    }

    @Transactional
    public Competencia atualizar(Long id, String nome) {
        Competencia competencia = buscarPorId(id);
        String nomeNormalizado = validarNome(nome);

        Optional<Competencia> existente =
                competenciaRepository.findByNome(nomeNormalizado);

        if (existente.isPresent()
                && !Objects.equals(existente.get().getIdCompetencia(), id)) {
            throw new IllegalArgumentException(
                    "Já existe outra competência com esse nome."
            );
        }

        competencia.setNome(nomeNormalizado);

        return competenciaRepository.save(competencia);
    }

    @Transactional
    public void excluir(Long id) {
        Competencia competencia = buscarPorId(id);
        competenciaRepository.delete(competencia);
    }

    private String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException(
                    "O nome da competência é obrigatório."
            );
        }

        return nome.trim();
    }
}