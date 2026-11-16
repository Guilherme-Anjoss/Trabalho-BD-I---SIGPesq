package br.ufes.sigpesq.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/** Parte de um financiamento destinada a um projeto (RF-08 / RF-22). */
public class AlocacaoFinanciamento {

    private Integer idAlocacao;

    @NotBlank(message = "Selecione o projeto.")
    private String codigoProjeto;

    @NotNull(message = "Selecione o financiamento.")
    private Integer idFinanciamento;

    @NotBlank(message = "A modalidade é obrigatória.")
    @Pattern(regexp = "Total|Parcial", message = "RN-08: A modalidade deve ser Total ou Parcial.")
    private String modalidade;

    @NotNull(message = "O valor destinado é obrigatório.")
    @DecimalMin(value = "0.01", message = "O valor destinado deve ser maior que zero.")
    private BigDecimal valorDestinado;

    // Preenchidos apenas nas listagens
    private String tituloProjeto;
    private String siglaAgencia;
    private String tipoFomento;

    public Integer getIdAlocacao() { return idAlocacao; }
    public void setIdAlocacao(Integer idAlocacao) { this.idAlocacao = idAlocacao; }
    public String getCodigoProjeto() { return codigoProjeto; }
    public void setCodigoProjeto(String codigoProjeto) { this.codigoProjeto = codigoProjeto; }
    public Integer getIdFinanciamento() { return idFinanciamento; }
    public void setIdFinanciamento(Integer idFinanciamento) { this.idFinanciamento = idFinanciamento; }
    public String getModalidade() { return modalidade; }
    public void setModalidade(String modalidade) { this.modalidade = modalidade; }
    public BigDecimal getValorDestinado() { return valorDestinado; }
    public void setValorDestinado(BigDecimal valorDestinado) { this.valorDestinado = valorDestinado; }
    public String getTituloProjeto() { return tituloProjeto; }
    public void setTituloProjeto(String tituloProjeto) { this.tituloProjeto = tituloProjeto; }
    public String getSiglaAgencia() { return siglaAgencia; }
    public void setSiglaAgencia(String siglaAgencia) { this.siglaAgencia = siglaAgencia; }
    public String getTipoFomento() { return tipoFomento; }
    public void setTipoFomento(String tipoFomento) { this.tipoFomento = tipoFomento; }
}
