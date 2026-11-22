package com.zg.acelera.mscompetencias.model;

import com.zg.acelera.mscompetencias.model.id.CandidatoCompetenciaId;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "candidato_competencia")
public class CandidatoCompetencia {

    @EmbeddedId
    private CandidatoCompetenciaId id;

    @MapsId("idCompetencia")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_competencia", nullable = false)
    private Competencia competencia;

    protected CandidatoCompetencia() {
    }

    public CandidatoCompetencia(String cpfCandidato, Competencia competencia) {
        this.competencia = Objects.requireNonNull(competencia);

        this.id = new CandidatoCompetenciaId(
                cpfCandidato.trim(),
                competencia.getIdCompetencia()
        );
    }

    public CandidatoCompetenciaId getId() {
        return id;
    }

    public Competencia getCompetencia() {
        return competencia;
    }

    public String getCpfCandidato() {
        return id.getCpfCandidato();
    }
}