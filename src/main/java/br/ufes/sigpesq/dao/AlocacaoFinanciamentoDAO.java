package br.ufes.sigpesq.dao;

import br.ufes.sigpesq.config.DatabaseConnection;
import br.ufes.sigpesq.model.AlocacaoFinanciamento;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AlocacaoFinanciamentoDAO {

    public List<AlocacaoFinanciamento> listar() {
        String sql = "SELECT al.id_alocacao, al.codigo_projeto, p.titulo, al.id_financiamento, "
                   + "ag.sigla, f.tipo_fomento, al.modalidade, al.valor_destinado "
                   + "FROM alocacao_financiamento al "
                   + "JOIN projeto_pesquisa p ON p.codigo = al.codigo_projeto "
                   + "JOIN financiamento f ON f.id_financiamento = al.id_financiamento "
                   + "JOIN agencia_fomento ag ON ag.id_agencia = f.id_agencia "
                   + "ORDER BY ag.sigla, al.codigo_projeto";
        List<AlocacaoFinanciamento> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AlocacaoFinanciamento a = new AlocacaoFinanciamento();
                a.setIdAlocacao(rs.getInt("id_alocacao"));
                a.setCodigoProjeto(rs.getString("codigo_projeto"));
                a.setTituloProjeto(rs.getString("titulo"));
                a.setIdFinanciamento(rs.getInt("id_financiamento"));
                a.setSiglaAgencia(rs.getString("sigla"));
                a.setTipoFomento(rs.getString("tipo_fomento"));
                a.setModalidade(rs.getString("modalidade"));
                a.setValorDestinado(rs.getBigDecimal("valor_destinado"));
                lista.add(a);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar alocações", e);
        }
        return lista;
    }

    /** Destina parte de um financiamento a um projeto (RF-08 / RF-22). O limite (RN-16) e checado por trigger. */
    public void inserir(AlocacaoFinanciamento a) {
        String sql = "INSERT INTO alocacao_financiamento (codigo_projeto, id_financiamento, modalidade, valor_destinado) "
                   + "VALUES (?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getCodigoProjeto());
            ps.setInt(2, a.getIdFinanciamento());
            ps.setString(3, a.getModalidade());
            ps.setBigDecimal(4, a.getValorDestinado());
            ps.executeUpdate();
            try (ResultSet chave = ps.getGeneratedKeys()) {
                if (chave.next()) a.setIdAlocacao(chave.getInt(1));
            }
        } catch (SQLException e) {
            throw ErrosBanco.traduzir(e, "Esse financiamento já foi alocado para este projeto.");
        }
    }
}
