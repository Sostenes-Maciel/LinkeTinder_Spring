package com.zg.acelera.mscompetencias.model;

import com.zg.acelera.mscompetencias.model.id.EmpresaCompetenciaId;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "empresa_competencia")
public class EmpresaCompetencia {

    @EmbeddedId
    private EmpresaCompetenciaId id;

    @MapsId("idCompetencia")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_competencia", nullable = false)
    private Competencia competencia;

    protected EmpresaCompetencia() {
    }

    public EmpresaCompetencia(String cnpjEmpresa, Competencia competencia) {
        this.competencia = Objects.requireNonNull(competencia);

        this.id = new EmpresaCompetenciaId(
                cnpjEmpresa,
                competencia.getIdCompetencia()
        );
    }

    public EmpresaCompetenciaId getId() {
        return id;
    }

    public Competencia getCompetencia() {
        return competencia;
    }

    public String getCnpjEmpresa() {
        return id.getCnpjEmpresa();
    }
}