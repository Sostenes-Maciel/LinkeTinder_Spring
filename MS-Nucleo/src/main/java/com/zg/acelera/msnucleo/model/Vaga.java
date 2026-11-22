package com.zg.acelera.msnucleo.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
        name = "vaga",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vaga_id_empresa",
                        columnNames = {"id_vaga", "cnpj_empresa"}
                )
        }
)
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vaga")
    private Long idVaga;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "pais", length = 100)
    private String pais;

    @Column(name = "estado", length = 100)
    private String estado;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    // Mapeamento direto da coluna que participa da chave composta.
    @Column(name = "cnpj_empresa", nullable = false, length = 14)
    private String cnpjEmpresa;

    // Relacionamento somente para leitura da coluna cnpj_empresa.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "cnpj_empresa",
            referencedColumnName = "cnpj",
            nullable = false,
            insertable = false,
            updatable = false
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Empresa empresa;

    protected Vaga() {
    }

    public Vaga(
            String titulo,
            String pais,
            String estado,
            String descricao,
            Empresa empresa
    ) {
        this.titulo = titulo;
        this.pais = pais;
        this.estado = estado;
        this.descricao = descricao;
        this.empresa = empresa;
        this.cnpjEmpresa = empresa.getCnpj();
    }

    public Long getIdVaga() {
        return idVaga;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getPais() {
        return pais;
    }

    public String getEstado() {
        return estado;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getCnpjEmpresa() {
        return cnpjEmpresa;
    }

    public Empresa getEmpresa() {
        return empresa;
    }
}