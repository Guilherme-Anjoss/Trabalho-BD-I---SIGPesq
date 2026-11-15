package br.ufes.sigpesq.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Agencia financiadora (CNPq, FAPES, CAPES...). */
public class AgenciaFomento {

    private Integer idAgencia;

    @NotBlank(message = "O nome da agência é obrigatório.")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
    private String nome;

    @NotBlank(message = "A sigla da agência é obrigatória.")
    @Size(max = 20, message = "A sigla deve ter no máximo 20 caracteres.")
    private String sigla;

    private boolean ativo = true;

    // Preenchidos apenas nas listagens
    private int qtdFinanciamentos;
    private java.math.BigDecimal totalFinanciado;

    public void normalizar() {
        if (nome != null) nome = nome.trim();
        if (sigla != null) sigla = sigla.trim();
    }

    public Integer getIdAgencia() { return idAgencia; }
    public void setIdAgencia(Integer idAgencia) { this.idAgencia = idAgencia; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSigla() { return sigla; }
    public void setSigla(String sigla) { this.sigla = sigla; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public int getQtdFinanciamentos() { return qtdFinanciamentos; }
    public void setQtdFinanciamentos(int qtdFinanciamentos) { this.qtdFinanciamentos = qtdFinanciamentos; }
    public java.math.BigDecimal getTotalFinanciado() { return totalFinanciado; }
    public void setTotalFinanciado(java.math.BigDecimal totalFinanciado) { this.totalFinanciado = totalFinanciado; }
}
