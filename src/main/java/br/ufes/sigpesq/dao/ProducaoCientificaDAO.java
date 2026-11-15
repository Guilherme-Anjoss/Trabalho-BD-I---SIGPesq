package br.ufes.sigpesq.dao;

import br.ufes.sigpesq.config.DatabaseConnection;
import br.ufes.sigpesq.model.ProducaoCientifica;
import br.ufes.sigpesq.model.TipoProducao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProducaoCientificaDAO {

    /** Catalogo de tipos de producao ativos (RN-15). */
    public List<TipoProducao> listarTipos() {
        String sql = "SELECT id_tipo_producao, nome, ativo FROM tipo_producao WHERE ativo = TRUE ORDER BY nome";
        List<TipoProducao> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TipoProducao t = new TipoProducao();
                t.setIdTipoProducao(rs.getInt("id_tipo_producao"));
                t.setNome(rs.getString("nome"));
                t.setAtivo(rs.getBoolean("ativo"));
                lista.add(t);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar tipos de produção", e);
        }
        return lista;
    }

    /**
     * Lista as producoes com tipo, projeto e autores em ordem (RF-12 / RF-13 / RF-21).
     * Filtros opcionais: ano de publicacao e projeto.
     */
    public List<ProducaoCientifica> listar(Integer ano, String codigoProjeto) {
        StringBuilder sql = new StringBuilder(
              "SELECT pc.id_producao, pc.codigo_projeto, p.titulo AS projeto, pc.id_tipo_producao, "
            + "tp.nome AS tipo, pc.titulo, pc.ano_publicacao, pc.meio_divulgacao, "
            + "GROUP_CONCAT(pa.nome ORDER BY a.ordem_autoria SEPARATOR ', ') AS autores "
            + "FROM producao_cientifica pc "
            + "JOIN tipo_producao tp ON tp.id_tipo_producao = pc.id_tipo_producao "
            + "JOIN projeto_pesquisa p ON p.codigo = pc.codigo_projeto "
            + "LEFT JOIN autoria a ON a.id_producao = pc.id_producao "
            + "LEFT JOIN participante pa ON pa.id_participante = a.id_participante "
            + "WHERE 1 = 1 ");
        if (ano != null) sql.append("AND pc.ano_publicacao = ? ");
        if (codigoProjeto != null) sql.append("AND pc.codigo_projeto = ? ");
        sql.append("GROUP BY pc.id_producao, pc.codigo_projeto, p.titulo, pc.id_tipo_producao, tp.nome, "
                 + "pc.titulo, pc.ano_publicacao, pc.meio_divulgacao "
                 + "ORDER BY pc.ano_publicacao DESC, pc.titulo");
        List<ProducaoCientifica> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int i = 1;
            if (ano != null) ps.setInt(i++, ano);
            if (codigoProjeto != null) ps.setString(i, codigoProjeto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProducaoCientifica pc = new ProducaoCientifica();
                    pc.setIdProducao(rs.getInt("id_producao"));
                    pc.setCodigoProjeto(rs.getString("codigo_projeto"));
                    pc.setTituloProjeto(rs.getString("projeto"));
                    pc.setIdTipoProducao(rs.getInt("id_tipo_producao"));
                    pc.setNomeTipo(rs.getString("tipo"));
                    pc.setTitulo(rs.getString("titulo"));
                    pc.setAnoPublicacao(rs.getInt("ano_publicacao"));
                    pc.setMeioDivulgacao(rs.getString("meio_divulgacao"));
                    pc.setNomesAutores(rs.getString("autores"));
                    lista.add(pc);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar produções", e);
        }
        return lista;
    }

    /**
     * Registra a producao e seus autores numa unica TRANSACAO (UC-06 inclui UC-07):
     * ou grava tudo, ou nao grava nada. Isso garante que nenhuma producao fique sem autor.
     */
    public void inserir(ProducaoCientifica pc) {
        String sqlProducao = "INSERT INTO producao_cientifica "
                + "(codigo_projeto, id_tipo_producao, titulo, ano_publicacao, meio_divulgacao) VALUES (?, ?, ?, ?, ?)";
        String sqlAutoria = "INSERT INTO autoria (id_producao, id_participante, ordem_autoria) VALUES (?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sqlProducao, PreparedStatement.RETURN_GENERATED_KEYS);
                 PreparedStatement psAutor = con.prepareStatement(sqlAutoria)) {
                ps.setString(1, pc.getCodigoProjeto());
                ps.setInt(2, pc.getIdTipoProducao());
                ps.setString(3, pc.getTitulo());
                ps.setInt(4, pc.getAnoPublicacao());
                ps.setString(5, pc.getMeioDivulgacao());
                ps.executeUpdate();
                try (ResultSet chave = ps.getGeneratedKeys()) {
                    if (!chave.next()) throw new SQLException("O banco não devolveu o id da produção.");
                    pc.setIdProducao(chave.getInt(1));
                }
                int ordem = 1;
                for (Integer idAutor : pc.getAutores()) {           // RN-06: FK para participante
                    psAutor.setInt(1, pc.getIdProducao());
                    psAutor.setInt(2, idAutor);
                    psAutor.setInt(3, ordem++);
                    psAutor.addBatch();
                }
                psAutor.executeBatch();
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw ErrosBanco.traduzir(e, "Autor repetido na mesma produção.");
        }
    }
}
