package sigpesq.dao;

import sigpesq.config.DatabaseConnection;
import sigpesq.model.Participante;
import sigpesq.model.ProjetoPesquisa;
import sigpesq.validation.ValidatorEntidade;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProjetoPesquisaDAO {

    // RF-01: Cadastrar novo projeto de pesquisa
    public void inserir(ProjetoPesquisa projeto) throws SQLException {
        ValidatorEntidade.validar(projeto);

        String sql = """
            INSERT INTO projeto_pesquisa
                (codigo, id_coordenador, titulo, descricao, data_inicio, data_termino, situacao, ativo)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, projeto.getCodigo());
            stmt.setInt(2, projeto.getIdCoordenador());
            stmt.setString(3, projeto.getTitulo());
            stmt.setString(4, projeto.getDescricao());
            stmt.setDate(5, Date.valueOf(projeto.getDataInicio()));
            stmt.setDate(6, Date.valueOf(projeto.getDataTermino()));
            stmt.setString(7, projeto.getSituacao());
            stmt.setBoolean(8, projeto.isAtivo());

            stmt.executeUpdate();
        }
    }

    // RF-02 / RF-15: Atualizar dados cadastrais e situacao de um projeto
    public void atualizar(ProjetoPesquisa projeto) throws SQLException {
        ValidatorEntidade.validar(projeto);

        String sql = """
            UPDATE projeto_pesquisa
               SET id_coordenador = ?,
                   titulo = ?,
                   descricao = ?,
                   data_inicio = ?,
                   data_termino = ?,
                   situacao = ?
             WHERE codigo = ? AND ativo = TRUE
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, projeto.getIdCoordenador());
            stmt.setString(2, projeto.getTitulo());
            stmt.setString(3, projeto.getDescricao());
            stmt.setDate(4, Date.valueOf(projeto.getDataInicio()));
            stmt.setDate(5, Date.valueOf(projeto.getDataTermino()));
            stmt.setString(6, projeto.getSituacao());
            stmt.setString(7, projeto.getCodigo());

            stmt.executeUpdate();
        }
    }

    // RF-24: Exclusao logica (inativacao) de um projeto
    public void inativar(String codigo) throws SQLException {
        String sql = "UPDATE projeto_pesquisa SET ativo = FALSE WHERE codigo = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, codigo);
            stmt.executeUpdate();
        }
    }

    // Listar todos os projetos ativos com o nome do respectivo coordenador (Roteiro / RF-05)
    public List<ProjetoPesquisa> listarAtivosComCoordenador() throws SQLException {
        String sql = """
            SELECT p.codigo, p.id_coordenador, p.titulo, p.descricao,
                   p.data_inicio, p.data_termino, p.situacao, p.ativo,
                   part.nome AS nome_coordenador
              FROM projeto_pesquisa p
              JOIN participante part ON p.id_coordenador = part.id_participante
             WHERE p.ativo = TRUE
             ORDER BY p.codigo
            """;

        List<ProjetoPesquisa> lista = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ProjetoPesquisa p = new ProjetoPesquisa(
                        rs.getString("codigo"),
                        rs.getInt("id_coordenador"),
                        rs.getString("titulo"),
                        rs.getString("descricao"),
                        rs.getDate("data_inicio").toLocalDate(),
                        rs.getDate("data_termino").toLocalDate(),
                        rs.getString("situacao"),
                        rs.getBoolean("ativo")
                );
                p.setNomeCoordenador(rs.getString("nome_coordenador"));
                lista.add(p);
            }
        }
        return lista;
    }

    // RF-03: Cadastrar participante (auxiliar para testes de coordenacao e vinculos)
    public void inserirParticipante(Participante participante) throws SQLException {
        ValidatorEntidade.validar(participante);

        String sql = """
            INSERT INTO participante (tipo, nome, cpf, email_institucional, ativo)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, participante.getTipo());
            stmt.setString(2, participante.getNome());
            stmt.setString(3, participante.getCpf());
            stmt.setString(4, participante.getEmailInstitucional());
            stmt.setBoolean(5, participante.isAtivo());

            stmt.executeUpdate();
        }
    }

    // Listar participantes ativos
    public List<Participante> listarParticipantesAtivos() throws SQLException {
        String sql = """
            SELECT id_participante, tipo, nome, cpf, email_institucional, ativo
              FROM participante
             WHERE ativo = TRUE
             ORDER BY id_participante
            """;

        List<Participante> lista = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(new Participante(
                        rs.getInt("id_participante"),
                        rs.getString("tipo"),
                        rs.getString("nome"),
                        rs.getString("cpf"),
                        rs.getString("email_institucional"),
                        rs.getBoolean("ativo")
                ));
            }
        }
        return lista;
    }
}