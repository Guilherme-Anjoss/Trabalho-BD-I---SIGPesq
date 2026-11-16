package br.ufes.sigpesq.model;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Vinculo de um participante com um projeto (tabela vinculo_participacao). */
public class VinculoParticipacao {

    private Integer idVinculo;

    @NotNull(message = "Selecione o participante.")
    private Integer idParticipante;

    @NotBlank(message = "Selecione o projeto.")
    private String codigoProjeto;

    @NotBlank(message = "RN-07: A função desempenhada é obrigatória.")
    @Size(max = 40, message = "A função deve ter no máximo 40 caracteres.")
    private String funcao;

    @NotNull(message = "RN-07: A data de entrada é obrigatória.")
    private LocalDate dataEntrada;

    /** Fica nula enquanto o vinculo esta ativo (RN-07 / RF-17). */
    private LocalDate dataSaida;

    // Preenchidos apenas nas listagens (JOIN)
    private String nomeParticipante;
    private String tipoParticipante;
    private String tituloProjeto;

    public void normalizar() {
        if (codigoProjeto != null) codigoProjeto = codigoProjeto.trim();
        if (funcao != null) funcao = funcao.trim();
    }

    @AssertTrue(message = "RN-11: A data de saída deve ser posterior à data de entrada.")
    public boolean isPeriodoValido() {
        if (dataEntrada == null || dataSaida == null) return true;
        return dataSaida.isAfter(dataEntrada);
    }

    public Integer getIdVinculo() { return idVinculo; }
    public void setIdVinculo(Integer idVinculo) { this.idVinculo = idVinculo; }
    public Integer getIdParticipante() { return idParticipante; }
    public void setIdParticipante(Integer idParticipante) { this.idParticipante = idParticipante; }
    public String getCodigoProjeto() { return codigoProjeto; }
    public void setCodigoProjeto(String codigoProjeto) { this.codigoProjeto = codigoProjeto; }
    public String getFuncao() { return funcao; }
    public void setFuncao(String funcao) { this.funcao = funcao; }
    public LocalDate getDataEntrada() { return dataEntrada; }
    public void setDataEntrada(LocalDate dataEntrada) { this.dataEntrada = dataEntrada; }
    public LocalDate getDataSaida() { return dataSaida; }
    public void setDataSaida(LocalDate dataSaida) { this.dataSaida = dataSaida; }
    public String getNomeParticipante() { return nomeParticipante; }
    public void setNomeParticipante(String nomeParticipante) { this.nomeParticipante = nomeParticipante; }
    public String getTipoParticipante() { return tipoParticipante; }
    public void setTipoParticipante(String tipoParticipante) { this.tipoParticipante = tipoParticipante; }
    public String getTituloProjeto() { return tituloProjeto; }
    public void setTituloProjeto(String tituloProjeto) { this.tituloProjeto = tituloProjeto; }
}
