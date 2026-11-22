package com.zg.acelera.mscompetencias.model.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EmpresaCompetenciaId implements Serializable {

    @Column(name = "cnpj_empresa", nullable = false, length = 14)
    private String cnpjEmpresa;

    @Column(name = "id_competencia", nullable = false)
    private Long idCompetencia;

    protected EmpresaCompetenciaId() {
    }

    public EmpresaCompetenciaId(String cnpjEmpresa, Long idCompetencia) {
        this.cnpjEmpresa = cnpjEmpresa;
        this.idCompetencia = idCompetencia;
    }

    public String getCnpjEmpresa() {
        return cnpjEmpresa;
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

        EmpresaCompetenciaId that = (EmpresaCompetenciaId) objeto;

        return Objects.equals(cnpjEmpresa, that.cnpjEmpresa)
                && Objects.equals(idCompetencia, that.idCompetencia);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cnpjEmpresa, idCompetencia);
    }
}