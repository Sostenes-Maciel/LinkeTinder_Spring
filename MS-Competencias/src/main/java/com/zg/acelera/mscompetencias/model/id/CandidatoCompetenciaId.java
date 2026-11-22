package com.zg.acelera.mscompetencias.model.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CandidatoCompetenciaId implements Serializable {

    @Column(name = "cpf_candidato", nullable = false, length = 14)
    private String cpfCandidato;

    @Column(name = "id_competencia", nullable = false)
    private Long idCompetencia;

    protected CandidatoCompetenciaId() {
    }

    public CandidatoCompetenciaId(String cpfCandidato, Long idCompetencia) {
        this.cpfCandidato = cpfCandidato;
        this.idCompetencia = idCompetencia;
    }

    public String getCpfCandidato() {
        return cpfCandidato;
    }

    public Long getIdCompetencia() {
        return idCompetencia;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) return true;

        if (objeto == null || getClass() != objeto.getClass()) {
            return false;
        }

        CandidatoCompetenciaId that = (CandidatoCompetenciaId) objeto;

        return Objects.equals(cpfCandidato, that.cpfCandidato)
                && Objects.equals(idCompetencia, that.idCompetencia);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpfCandidato, idCompetencia);
    }
}