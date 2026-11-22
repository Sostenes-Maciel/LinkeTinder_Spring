package com.zg.acelera.mscompetencias.model.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class VagaCompetenciaId implements Serializable {

    @Column(name = "id_vaga", nullable = false)
    private Long idVaga;

    @Column(name = "id_competencia", nullable = false)
    private Long idCompetencia;

    protected VagaCompetenciaId() {
    }

    public VagaCompetenciaId(Long idVaga, Long idCompetencia) {
        this.idVaga = idVaga;
        this.idCompetencia = idCompetencia;
    }

    public Long getIdVaga() {
        return idVaga;
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

        VagaCompetenciaId that = (VagaCompetenciaId) objeto;

        return Objects.equals(idVaga, that.idVaga)
                && Objects.equals(idCompetencia, that.idCompetencia);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idVaga, idCompetencia);
    }
}