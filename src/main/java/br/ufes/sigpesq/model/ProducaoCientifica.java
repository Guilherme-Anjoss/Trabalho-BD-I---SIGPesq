package br.ufes.sigpesq.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/** Producao cientifica gerada por um projeto (RF-10 / RF-11). */
public class ProducaoCientifica {

    private Integer idProducao;

    @NotBlank(message = "Selecione o projeto que gerou a produção.")
    private String codigoProjeto;

    @NotNull(message = "Selecione o tipo de produção.")
    private Integer idTipoProducao;

    @NotBlank(message = "O título é obrigatório.")
    @Size(max = 200, message = "O título deve ter no máximo 200 caracteres.")
    private String titulo;

    @NotNull(message = "O ano de publicação é obrigatório.")
    @Min(value = 1900, message = "Ano de publicação inválido.")
    @Max(value = 2100, message = "Ano de publicação inválido.")
    private Integer anoPublicacao;

    @NotBlank(message = "O meio de divulgação é obrigatório.")
    @Size(max = 150, message = "O meio de divulgação deve ter no máximo 150 caracteres.")
    private String meioDivulgacao;

    /** Ids dos participantes autores, na ordem de autoria (RN-06: pelo menos um). */
    @NotEmpty(message = "RN-06: Informe pelo menos um autor cadastrado como participante.")
    private List<Integer> autores = new ArrayList<>();

    // Preenchidos apenas nas listagens
    private String nomeTipo;
    private String tituloProjeto;
    private String nomesAutores;

    public void normalizar() {
        if (codigoProjeto != null) codigoProjeto = codigoProjeto.trim();
        if (titulo != null) titulo = titulo.trim();
        if (meioDivulgacao != null) meioDivulgacao = meioDivulgacao.trim();
        if (autores != null) autores = new ArrayList<>(new java.util.LinkedHashSet<>(autores)); // remove repetidos
    }

    public Integer getIdProducao() { return idProducao; }
    public void setIdProducao(Integer idProducao) { this.idProducao = idProducao; }
    public String getCodigoProjeto() { return codigoProjeto; }
    public void setCodigoProjeto(String codigoProjeto) { this.codigoProjeto = codigoProjeto; }
    public Integer getIdTipoProducao() { return idTipoProducao; }
    public void setIdTipoProducao(Integer idTipoProducao) { this.idTipoProducao = idTipoProducao; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public Integer getAnoPublicacao() { return anoPublicacao; }
    public void setAnoPublicacao(Integer anoPublicacao) { this.anoPublicacao = anoPublicacao; }
    public String getMeioDivulgacao() { return meioDivulgacao; }
    public void setMeioDivulgacao(String meioDivulgacao) { this.meioDivulgacao = meioDivulgacao; }
    public List<Integer> getAutores() { return autores; }
    public void setAutores(List<Integer> autores) { this.autores = autores; }
    public String getNomeTipo() { return nomeTipo; }
    public void setNomeTipo(String nomeTipo) { this.nomeTipo = nomeTipo; }
    public String getTituloProjeto() { return tituloProjeto; }
    public void setTituloProjeto(String tituloProjeto) { this.tituloProjeto = tituloProjeto; }
    public String getNomesAutores() { return nomesAutores; }
    public void setNomesAutores(String nomesAutores) { this.nomesAutores = nomesAutores; }
}
