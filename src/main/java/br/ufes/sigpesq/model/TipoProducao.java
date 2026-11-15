package br.ufes.sigpesq.model;

/** Catalogo extensivel de tipos de producao (RN-15). */
public class TipoProducao {

    private Integer idTipoProducao;
    private String nome;
    private boolean ativo = true;

    public Integer getIdTipoProducao() { return idTipoProducao; }
    public void setIdTipoProducao(Integer idTipoProducao) { this.idTipoProducao = idTipoProducao; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
