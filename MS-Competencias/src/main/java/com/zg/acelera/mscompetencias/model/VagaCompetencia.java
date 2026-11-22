package com.zg.acelera.mscompetencias.model;

import com.zg.acelera.mscompetencias.model.id.VagaCompetenciaId;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "vaga_competencia")
public class VagaCompetencia {

    @EmbeddedId
    private VagaCompetenciaId id;

    @MapsId("idCompetencia")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_competencia", nullable = false)
    private Competencia competencia;

    protected VagaCompetencia() {
    }

    public VagaCompetencia(Long idVaga, Competencia competencia) {
        this.competencia = Objects.requireNonNull(competencia);

        this.id = new VagaCompetenciaId(
                idVaga,
                competencia.getIdCompetencia()
        );
    }

    public VagaCompetenciaId getId() {
        return id;
    }

    public Competencia getCompetencia() {
        return competencia;
    }

    public Long getIdVaga() {
        return id.getIdVaga();
    }
}