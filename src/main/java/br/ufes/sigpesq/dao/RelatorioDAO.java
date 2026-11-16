package br.ufes.sigpesq.dao;

import br.ufes.sigpesq.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas de relatorio pedidas no roteiro do trabalho (as mesmas de sigpesq_consultas.sql).
 * Cada linha volta como um mapa coluna -> valor, pronto para virar JSON.
 */
public class RelatorioDAO {

    /** Numeros do painel "Visao geral". */
    public Map<String, Object> resumo() {
        String sql = "SELECT "
            + "(SELECT COUNT(*) FROM projeto_pesquisa WHERE ativo = TRUE AND situacao = 'Em andamento') AS projetosEmAndamento, "
            + "(SELECT COUNT(*) FROM projeto_pesquisa WHERE ativo = TRUE) AS projetos, "
            + "(SELECT COUNT(*) FROM participante WHERE ativo = TRUE) AS participantes, "
            + "(SELECT COUNT(*) FROM vinculo_participacao WHERE data_saida IS NULL) AS vinculosAtivos, "
            + "(SELECT COUNT(*) FROM producao_cientifica) AS producoes, "
            + "(SELECT COALESCE(SUM(valor_total), 0) FROM financiamento WHERE ativo = TRUE) AS totalFinanciado, "
            + "(SELECT COALESCE(SUM(valor_destinado), 0) FROM alocacao_financiamento) AS totalAlocado";
        return consultar(sql).get(0);
    }

    /** Roteiro: listar todos os projetos com seus coordenadores (RF-05 quando filtrado por docente). */
    public List<Map<String, Object>> projetosComCoordenadores(Integer idDocente) {
        String sql = "SELECT p.codigo, p.titulo, c.nome AS coordenador, p.situacao, p.data_inicio, p.data_termino "
                   + "FROM projeto_pesquisa p JOIN participante c ON c.id_participante = p.id_coordenador "
                   + "WHERE p.ativo = TRUE " + (idDocente != null ? "AND c.id_participante = ? " : "")
                   + "ORDER BY c.nome, p.codigo";
        return idDocente != null ? consultar(sql, idDocente) : consultar(sql);
    }

    /** Roteiro: producoes cientificas por tipo e ano (RF-13 / RF-21). */
    public List<Map<String, Object>> producoesPorTipoEAno() {
        String sql = "SELECT pc.ano_publicacao AS ano, tp.nome AS tipo, COUNT(*) AS quantidade "
                   + "FROM producao_cientifica pc JOIN tipo_producao tp ON tp.id_tipo_producao = pc.id_tipo_producao "
                   + "GROUP BY pc.ano_publicacao, tp.nome ORDER BY ano DESC, quantidade DESC";
        return consultar(sql);
    }

    /** Roteiro: total de financiamento por agencia (RF-20). LEFT JOIN mostra agencia sem financiamento com 0. */
    public List<Map<String, Object>> totalPorAgencia() {
        String sql = "SELECT ag.sigla, ag.nome, COUNT(f.id_financiamento) AS qtdFinanciamentos, "
                   + "COALESCE(SUM(f.valor_total), 0) AS totalFinanciado "
                   + "FROM agencia_fomento ag "
                   + "LEFT JOIN financiamento f ON f.id_agencia = ag.id_agencia AND f.ativo = TRUE "
                   + "WHERE ag.ativo = TRUE GROUP BY ag.id_agencia, ag.sigla, ag.nome ORDER BY totalFinanciado DESC";
        return consultar(sql);
    }

    /** Roteiro: participantes que atuam em mais de um projeto (RF-14). */
    public List<Map<String, Object>> participantesEmVariosProjetos() {
        String sql = "SELECT pt.nome, pt.tipo, COUNT(DISTINCT v.codigo_projeto) AS qtdProjetos, "
                   + "GROUP_CONCAT(DISTINCT v.codigo_projeto ORDER BY v.codigo_projeto SEPARATOR ', ') AS projetos "
                   + "FROM vinculo_participacao v JOIN participante pt ON pt.id_participante = v.id_participante "
                   + "WHERE v.data_saida IS NULL "
                   + "GROUP BY pt.id_participante, pt.nome, pt.tipo "
                   + "HAVING COUNT(DISTINCT v.codigo_projeto) > 1 ORDER BY qtdProjetos DESC, pt.nome";
        return consultar(sql);
    }

    /** Executa um SELECT e devolve as linhas como mapas (datas viram texto AAAA-MM-DD). */
    private List<Map<String, Object>> consultar(String sql, Object... parametros) {
        List<Map<String, Object>> linhas = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) ps.setObject(i + 1, parametros[i]);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> linha = new LinkedHashMap<>();
                    for (int c = 1; c <= meta.getColumnCount(); c++) {
                        Object valor = rs.getObject(c);
                        if (valor instanceof java.sql.Date d) valor = d.toLocalDate().toString();
                        if (valor instanceof java.time.LocalDate d) valor = d.toString();
                        linha.put(meta.getColumnLabel(c), valor);
                    }
                    linhas.add(linha);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao executar relatório", e);
        }
        return linhas;
    }
}
