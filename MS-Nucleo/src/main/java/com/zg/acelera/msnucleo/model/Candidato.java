package com.zg.acelera.msnucleo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "candidato")
public class Candidato {

    @Id
    @Column(name = "cpf", nullable = false, length = 11)
    private String cpf;

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "pais", length = 100)
    private String pais;

    @Column(name = "estado", length = 100)
    private String estado;

    @Column(name = "cep", length = 20)
    private String cep;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "idade")
    private Integer idade;

    protected Candidato() {
    }

    public Candidato(
            String cpf,
            String nome,
            String email,
            String pais,
            String estado,
            String cep,
            String descricao,
            Integer idade
    ) {
        this.cpf = cpf;
        this.nome = nome;
        this.email = email;
        this.pais = pais;
        this.estado = estado;
        this.cep = cep;
        this.descricao = descricao;
        this.idade = idade;
    }

    public String getCpf() {
        return cpf;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getPais() {
        return pais;
    }

    public String getEstado() {
        return estado;
    }

    public String getCep() {
        return cep;
    }

    public String getDescricao() {
        return descricao;
    }

    public Integer getIdade() {
        return idade;
    }

    public void atualizarDados(
            String nome,
            String email,
            String pais,
            String estado,
            String cep,
            String descricao,
            Integer idade
    ) {
        this.nome = nome;
        this.email = email;
        this.pais = pais;
        this.estado = estado;
        this.cep = cep;
        this.descricao = descricao;
        this.idade = idade;
    }
}