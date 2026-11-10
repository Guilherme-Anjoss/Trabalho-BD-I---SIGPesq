package sigpesq.model;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class ProjetoPesquisa {

    @NotBlank(message = "RN-04: O codigo do projeto eh obrigatorio.")
    @Size(max = 20, message = "O codigo do projeto deve ter no maximo 20 caracteres.")
    private String codigo;

    @NotNull(message = "RN-01: O coordenador principal eh obrigatorio.")
    @Positive(message = "O ID do coordenador deve ser um numero positivo.")
    private Integer idCoordenador;

    @NotBlank(message = "RF-01: O titulo do projeto eh obrigatorio.")
    @Size(max = 200, message = "O titulo deve ter no maximo 200 caracteres.")
    private String titulo;

    @NotBlank(message = "RF-01: A descricao do projeto eh obrigatoria.")
    private String descricao;

    @NotNull(message = "RF-01: A data de inicio eh obrigatoria.")
    private LocalDate dataInicio;

    @NotNull(message = "RF-01: A data de termino eh obrigatoria.")
    private LocalDate dataTermino;

    @NotBlank(message = "RN-03: A situacao do projeto eh obrigatoria.")
    @Pattern(
        regexp = "Em andamento|Concluido|Cancelado",
        message = "RN-03: A situacao deve ser 'Em andamento', 'Concluido' ou 'Cancelado' (sem acento, conforme constraint do banco)."
    )
    private String situacao = "Em andamento";

    private boolean ativo = true;

    // Campo auxiliar opcional para exibir o nome do docente coordenador em consultas JOIN
    private String nomeCoordenador;

    public ProjetoPesquisa() {
    }

    public ProjetoPesquisa(String codigo, Integer idCoordenador, String titulo, String descricao,
                           LocalDate dataInicio, LocalDate dataTermino, String situacao, boolean ativo) {
        this.codigo = codigo;
        this.idCoordenador = idCoordenador;
        this.titulo = titulo;
        this.descricao = descricao;
        this.dataInicio = dataInicio;
        this.dataTermino = dataTermino;
        this.situacao = situacao;
        this.ativo = ativo;
    }

    @AssertTrue(message = "RN-10: A data de termino deve ser estritamente posterior a data de inicio.")
    public boolean isPeriodoValido() {
        if (dataInicio == null || dataTermino == null) {
            return true; // A nulidade ja eh tratada por @NotNull
        }
        return dataTermino.isAfter(dataInicio);
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Integer getIdCoordenador() {
        return idCoordenador;
    }

    public void setIdCoordenador(Integer idCoordenador) {
        this.idCoordenador = idCoordenador;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataTermino() {
        return dataTermino;
    }

    public void setDataTermino(LocalDate dataTermino) {
        this.dataTermino = dataTermino;
    }

    public String getSituacao() {
        return situacao;
    }

    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public String getNomeCoordenador() {
        return nomeCoordenador;
    }

    public void setNomeCoordenador(String nomeCoordenador) {
        this.nomeCoordenador = nomeCoordenador;
    }

    @Override
    public String toString() {
        String coord = (nomeCoordenador != null) ? nomeCoordenador : "ID " + idCoordenador;
        return String.format("[%s] %s | Situacao: %s | Vigencia: %s a %s | Coordenador: %s",
                codigo, titulo, situacao, dataInicio, dataTermino, coord);
    }
}