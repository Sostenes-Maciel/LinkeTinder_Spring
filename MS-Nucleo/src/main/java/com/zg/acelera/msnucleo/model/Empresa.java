package com.zg.acelera.msnucleo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "empresa")
public class Empresa {

    @Id
    @Column(name = "cnpj", nullable = false, length = 14)
    private String cnpj;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "pais", length = 100)
    private String pais;

    @Column(name = "estado", length = 100)
    private String estado;

    @Column(name = "cep", length = 20)
    private String cep;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    protected Empresa() {
    }

    public Empresa(
            String cnpj,
            String nome,
            String email,
            String pais,
            String estado,
            String cep,
            String descricao
    ) {
        this.cnpj = cnpj;
        this.nome = nome;
        this.email = email;
        this.pais = pais;
        this.estado = estado;
        this.cep = cep;
        this.descricao = descricao;
    }

    public String getCnpj() {
        return cnpj;
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

    public void atualizarDados(
            String nome,
            String email,
            String pais,
            String estado,
            String cep,
            String descricao
    ) {
        this.nome = nome;
        this.email = email;
        this.pais = pais;
        this.estado = estado;
        this.cep = cep;
        this.descricao = descricao;
    }
}