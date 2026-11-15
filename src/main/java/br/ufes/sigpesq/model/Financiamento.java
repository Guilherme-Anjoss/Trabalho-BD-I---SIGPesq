package br.ufes.sigpesq.model;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Financiamento concedido por uma agencia (RF-07). */
public class Financiamento {

    private Integer idFinanciamento;

    @NotNull(message = "Selecione a agência financiadora.")
    private Integer idAgencia;

    @NotBlank(message = "O tipo de fomento é obrigatório.")
    @Size(max = 50, message = "O tipo de fomento deve ter no máximo 50 caracteres.")
    private String tipoFomento;

    @NotNull(message = "O início da vigência é obrigatório.")
    private LocalDate inicioVigencia;

    @NotNull(message = "O fim da vigência é obrigatório.")
    private LocalDate fimVigencia;

    @NotNull(message = "O valor total é obrigatório.")
    @DecimalMin(value = "0.01", message = "O valor total deve ser maior que zero.")
    private BigDecimal valorTotal;

    private boolean ativo = true;

    // Preenchidos apenas nas listagens
    private String siglaAgencia;
    private BigDecimal valorAlocado;

    public void normalizar() {
        if (tipoFomento != null) tipoFomento = tipoFomento.trim();
    }

    @AssertTrue(message = "RN-12: O fim da vigência deve ser posterior ao início.")
    public boolean isVigenciaValida() {
        if (inicioVigencia == null || fimVigencia == null) return true;
        return fimVigencia.isAfter(inicioVigencia);
    }

    public Integer getIdFinanciamento() { return idFinanciamento; }
    public void setIdFinanciamento(Integer idFinanciamento) { this.idFinanciamento = idFinanciamento; }
    public Integer getIdAgencia() { return idAgencia; }
    public void setIdAgencia(Integer idAgencia) { this.idAgencia = idAgencia; }
    public String getTipoFomento() { return tipoFomento; }
    public void setTipoFomento(String tipoFomento) { this.tipoFomento = tipoFomento; }
    public LocalDate getInicioVigencia() { return inicioVigencia; }
    public void setInicioVigencia(LocalDate inicioVigencia) { this.inicioVigencia = inicioVigencia; }
    public LocalDate getFimVigencia() { return fimVigencia; }
    public void setFimVigencia(LocalDate fimVigencia) { this.fimVigencia = fimVigencia; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public String getSiglaAgencia() { return siglaAgencia; }
    public void setSiglaAgencia(String siglaAgencia) { this.siglaAgencia = siglaAgencia; }
    public BigDecimal getValorAlocado() { return valorAlocado; }
    public void setValorAlocado(BigDecimal valorAlocado) { this.valorAlocado = valorAlocado; }
}
