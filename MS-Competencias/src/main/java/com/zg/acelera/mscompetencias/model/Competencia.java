package com.zg.acelera.mscompetencias.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "competencia",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_competencia_nome",
                        columnNames = "nome"
                )
        }
)
public class Competencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_competencia")
    private Long idCompetencia;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    protected Competencia() {
    }

    public Competencia(String nome) {
        this.nome = nome;
    }

    public Long getIdCompetencia() {
        return idCompetencia;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}