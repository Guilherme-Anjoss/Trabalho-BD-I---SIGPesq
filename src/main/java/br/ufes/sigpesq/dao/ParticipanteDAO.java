package br.ufes.sigpesq.dao;

import br.ufes.sigpesq.config.DatabaseConnection;
import br.ufes.sigpesq.model.Participante;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ParticipanteDAO {

    /** Docentes ativos: so eles podem ser coordenadores (RN-02). */
    public List<Participante> listarDocentesAtivos() {
        String sql = "SELECT id_participante, nome FROM participante "
                   + "WHERE tipo = 'Docente' AND ativo = TRUE ORDER BY nome";
        List<Participante> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Participante p = new Participante();
                p.setIdParticipante(rs.getInt("id_participante"));
                p.setNome(rs.getString("nome"));
                p.setTipo("Docente");
                lista.add(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar docentes", e);
        }
        return lista;
    }

    /**
     * Lista os participantes ativos (RF-03).
     * O CPF sai mascarado (***.222.333-**) para proteger o dado pessoal (RNF-03 / LGPD).
     */
    public List<Participante> listar() {
        String sql = "SELECT id_participante, tipo, nome, cpf, email_institucional, ativo "
                   + "FROM participante WHERE ativo = TRUE ORDER BY nome";
        List<Participante> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Participante p = new Participante();
                p.setIdParticipante(rs.getInt("id_participante"));
                p.setTipo(rs.getString("tipo"));
                p.setNome(rs.getString("nome"));
                p.setCpf(mascararCpf(rs.getString("cpf")));
                p.setEmailInstitucional(rs.getString("email_institucional"));
                p.setAtivo(rs.getBoolean("ativo"));
                lista.add(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar participantes", e);
        }
        return lista;
    }

    /** Cadastra um participante (RF-03). CPF e e-mail unicos sao garantidos pelo banco (RN-09, RN-14). */
    public void inserir(Participante p) {
        String sql = "INSERT INTO participante (tipo, nome, cpf, email_institucional) VALUES (?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getTipo());
            ps.setString(2, p.getNome());
            ps.setString(3, p.getCpf());
            ps.setString(4, p.getEmailInstitucional());
            ps.executeUpdate();
            try (ResultSet chave = ps.getGeneratedKeys()) {
                if (chave.next()) p.setIdParticipante(chave.getInt(1));
            }
        } catch (SQLException e) {
            String msg = e.getMessage() != null && e.getMessage().contains("email")
                    ? "RN-14: já existe um participante com esse e-mail institucional."
                    : "RN-09: já existe um participante com esse CPF.";
            throw ErrosBanco.traduzir(e, msg);
        }
        p.setCpf(mascararCpf(p.getCpf()));
    }

    /** Exclusao logica (RF-24): o registro continua no banco para preservar o historico. */
    public void inativar(int idParticipante) {
        String sql = "UPDATE participante SET ativo = FALSE WHERE id_participante = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idParticipante);
            if (ps.executeUpdate() == 0) throw new IllegalArgumentException("Participante não encontrado.");
        } catch (SQLException e) {
            throw ErrosBanco.traduzir(e, "Erro ao inativar participante.");
        }
    }

    static String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;
        return "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
    }
}
