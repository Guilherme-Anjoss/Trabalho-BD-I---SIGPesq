package br.ufes.sigpesq.dao;

import br.ufes.sigpesq.config.DatabaseConnection;
import br.ufes.sigpesq.model.VinculoParticipacao;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VinculoParticipacaoDAO {

    /**
     * Lista os vinculos (RF-06). Se codigoProjeto for informado, mostra so os
     * participantes daquele projeto (UC-09).
     */
    public List<VinculoParticipacao> listar(String codigoProjeto) {
        String sql = "SELECT v.id_vinculo, v.id_participante, v.codigo_projeto, v.funcao, "
                   + "v.data_entrada, v.data_saida, pt.nome, pt.tipo, p.titulo "
                   + "FROM vinculo_participacao v "
                   + "JOIN participante pt ON pt.id_participante = v.id_participante "
                   + "JOIN projeto_pesquisa p ON p.codigo = v.codigo_projeto "
                   + (codigoProjeto != null ? "WHERE v.codigo_projeto = ? " : "")
                   + "ORDER BY v.codigo_projeto, v.data_entrada";
        List<VinculoParticipacao> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (codigoProjeto != null) ps.setString(1, codigoProjeto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VinculoParticipacao v = new VinculoParticipacao();
                    v.setIdVinculo(rs.getInt("id_vinculo"));
                    v.setIdParticipante(rs.getInt("id_participante"));
                    v.setCodigoProjeto(rs.getString("codigo_projeto"));
                    v.setFuncao(rs.getString("funcao"));
                    v.setDataEntrada(rs.getDate("data_entrada").toLocalDate());
                    Date saida = rs.getDate("data_saida");
                    v.setDataSaida(saida != null ? saida.toLocalDate() : null);
                    v.setNomeParticipante(rs.getString("nome"));
                    v.setTipoParticipante(rs.getString("tipo"));
                    v.setTituloProjeto(rs.getString("titulo"));
                    lista.add(v);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar vínculos", e);
        }
        return lista;
    }

    /** Vincula um participante a um projeto (RF-04). RN-17, RN-19 e RN-01 sao checadas pelo banco. */
    public void inserir(VinculoParticipacao v) {
        String sql = "INSERT INTO vinculo_participacao "
                   + "(id_participante, codigo_projeto, funcao, data_entrada, data_saida) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, v.getIdParticipante());
            ps.setString(2, v.getCodigoProjeto());
            ps.setString(3, v.getFuncao());
            ps.setDate(4, Date.valueOf(v.getDataEntrada()));
            ps.setDate(5, v.getDataSaida() != null ? Date.valueOf(v.getDataSaida()) : null);
            ps.executeUpdate();
            try (ResultSet chave = ps.getGeneratedKeys()) {
                if (chave.next()) v.setIdVinculo(chave.getInt(1));
            }
        } catch (SQLException e) {
            throw ErrosBanco.traduzir(e,
                "RN-19: esse participante já tem um vínculo ativo com a mesma função neste projeto.");
        }
    }

    /** Encerra um vinculo ativo registrando a data de saida (RF-17). */
    public void encerrar(int idVinculo, LocalDate dataSaida) {
        String sql = "UPDATE vinculo_participacao SET data_saida = ? WHERE id_vinculo = ? AND data_saida IS NULL";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(dataSaida));
            ps.setInt(2, idVinculo);
            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("Vínculo não encontrado ou já encerrado.");
            }
        } catch (SQLException e) {
            throw ErrosBanco.traduzir(e, "Erro ao encerrar vínculo.");
        }
    }
}
