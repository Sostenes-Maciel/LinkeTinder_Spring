package com.zg.acelera.msnucleo.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
        name = "match_vaga",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_match_candidato_empresa_vaga",
                        columnNames = {
                                "cpf_candidato",
                                "cnpj_empresa",
                                "id_vaga"
                        }
                )
        }
)
public class MatchVaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_match")
    private Long idMatch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "cpf_candidato",
            referencedColumnName = "cpf",
            nullable = false
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Candidato candidato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns(
            value = {
                    @JoinColumn(
                            name = "id_vaga",
                            referencedColumnName = "id_vaga",
                            nullable = false
                    ),
                    @JoinColumn(
                            name = "cnpj_empresa",
                            referencedColumnName = "cnpj_empresa",
                            nullable = false
                    )
            },
            foreignKey = @ForeignKey(name = "fk_match_vaga_empresa")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Vaga vaga;

    protected MatchVaga() {
    }

    public MatchVaga(Candidato candidato, Vaga vaga) {
        this.candidato = candidato;
        this.vaga = vaga;
    }

    public Long getIdMatch() {
        return idMatch;
    }

    public Candidato getCandidato() {
        return candidato;
    }

    public Vaga getVaga() {
        return vaga;
    }

    public Empresa getEmpresa() {
        return vaga != null ? vaga.getEmpresa() : null;
    }
}