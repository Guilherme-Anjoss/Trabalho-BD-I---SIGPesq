package br.ufes.sigpesq.dao;

import br.ufes.sigpesq.config.DatabaseConnection;
import br.ufes.sigpesq.model.Financiamento;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FinanciamentoDAO {

    /** Lista os financiamentos ativos com a agencia e o quanto ja foi alocado (saldo = total - alocado). */
    public List<Financiamento> listar() {
        String sql = "SELECT f.id_financiamento, f.id_agencia, ag.sigla, f.tipo_fomento, "
                   + "f.inicio_vigencia, f.fim_vigencia, f.valor_total, f.ativo, "
                   + "COALESCE(SUM(al.valor_destinado), 0) AS alocado "
                   + "FROM financiamento f "
                   + "JOIN agencia_fomento ag ON ag.id_agencia = f.id_agencia "
                   + "LEFT JOIN alocacao_financiamento al ON al.id_financiamento = f.id_financiamento "
                   + "WHERE f.ativo = TRUE "
                   + "GROUP BY f.id_financiamento, f.id_agencia, ag.sigla, f.tipo_fomento, "
                   + "f.inicio_vigencia, f.fim_vigencia, f.valor_total, f.ativo "
                   + "ORDER BY ag.sigla, f.inicio_vigencia";
        List<Financiamento> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Financiamento f = new Financiamento();
                f.setIdFinanciamento(rs.getInt("id_financiamento"));
                f.setIdAgencia(rs.getInt("id_agencia"));
                f.setSiglaAgencia(rs.getString("sigla"));
                f.setTipoFomento(rs.getString("tipo_fomento"));
                f.setInicioVigencia(rs.getDate("inicio_vigencia").toLocalDate());
                f.setFimVigencia(rs.getDate("fim_vigencia").toLocalDate());
                f.setValorTotal(rs.getBigDecimal("valor_total"));
                f.setAtivo(rs.getBoolean("ativo"));
                f.setValorAlocado(rs.getBigDecimal("alocado"));
                lista.add(f);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar financiamentos", e);
        }
        return lista;
    }

    /** Cadastra um financiamento (RF-07). RN-12 (vigencia) e valor > 0 sao CHECKs do banco. */
    public void inserir(Financiamento f) {
        String sql = "INSERT INTO financiamento (id_agencia, tipo_fomento, inicio_vigencia, fim_vigencia, valor_total) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, f.getIdAgencia());
            ps.setString(2, f.getTipoFomento());
            ps.setDate(3, Date.valueOf(f.getInicioVigencia()));
            ps.setDate(4, Date.valueOf(f.getFimVigencia()));
            ps.setBigDecimal(5, f.getValorTotal());
            ps.executeUpdate();
            try (ResultSet chave = ps.getGeneratedKeys()) {
                if (chave.next()) f.setIdFinanciamento(chave.getInt(1));
            }
        } catch (SQLException e) {
            throw ErrosBanco.traduzir(e, "Financiamento duplicado.");
        }
    }
}
