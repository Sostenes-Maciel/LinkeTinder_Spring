package com.zg.acelera.msnucleo.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "curtida")
@Check(
        constraints = "id_vaga IS NOT NULL OR cnpj_empresa IS NOT NULL"
)
public class Curtida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_curtida")
    private Long idCurtida;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "cpf_candidato",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_curtida_candidato")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Candidato candidato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "id_vaga",
            foreignKey = @ForeignKey(name = "fk_curtida_vaga")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Vaga vaga;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "cnpj_empresa",
            foreignKey = @ForeignKey(name = "fk_curtida_empresa")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Empresa empresa;

    protected Curtida() {
    }

    // Curtida realizada pelo candidato em uma vaga.
    public Curtida(Candidato candidato, Vaga vaga) {
        this.candidato = candidato;
        this.vaga = vaga;
    }

    public Curtida(Candidato candidato, Vaga vaga, Empresa empresa) {
        this.candidato = candidato;
        this.vaga = vaga;
        this.empresa = empresa;
    }

    public Long getIdCurtida() {
        return idCurtida;
    }

    public Candidato getCandidato() {
        return candidato;
    }

    public Vaga getVaga() {
        return vaga;
    }

    public Empresa getEmpresa() {
        return empresa;
    }
}