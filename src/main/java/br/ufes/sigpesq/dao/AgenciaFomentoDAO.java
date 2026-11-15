package br.ufes.sigpesq.dao;

import br.ufes.sigpesq.config.DatabaseConnection;
import br.ufes.sigpesq.model.AgenciaFomento;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AgenciaFomentoDAO {

    /** Lista as agencias com a quantidade e o total financiado (RF-09 / RF-20). */
    public List<AgenciaFomento> listar() {
        String sql = "SELECT ag.id_agencia, ag.nome, ag.sigla, ag.ativo, "
                   + "COUNT(f.id_financiamento) AS qtd, COALESCE(SUM(f.valor_total), 0) AS total "
                   + "FROM agencia_fomento ag "
                   + "LEFT JOIN financiamento f ON f.id_agencia = ag.id_agencia AND f.ativo = TRUE "
                   + "WHERE ag.ativo = TRUE "
                   + "GROUP BY ag.id_agencia, ag.nome, ag.sigla, ag.ativo ORDER BY ag.sigla";
        List<AgenciaFomento> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AgenciaFomento a = new AgenciaFomento();
                a.setIdAgencia(rs.getInt("id_agencia"));
                a.setNome(rs.getString("nome"));
                a.setSigla(rs.getString("sigla"));
                a.setAtivo(rs.getBoolean("ativo"));
                a.setQtdFinanciamentos(rs.getInt("qtd"));
                a.setTotalFinanciado(rs.getBigDecimal("total"));
                lista.add(a);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar agências", e);
        }
        return lista;
    }

    /** Cadastra uma agencia de fomento (RF-07). */
    public void inserir(AgenciaFomento a) {
        String sql = "INSERT INTO agencia_fomento (nome, sigla) VALUES (?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getNome());
            ps.setString(2, a.getSigla());
            ps.executeUpdate();
            try (ResultSet chave = ps.getGeneratedKeys()) {
                if (chave.next()) a.setIdAgencia(chave.getInt(1));
            }
        } catch (SQLException e) {
            throw ErrosBanco.traduzir(e, "Já existe uma agência com essa sigla.");
        }
    }
}
