package sigpesq;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import sigpesq.dao.ProjetoPesquisaDAO;
import sigpesq.model.Participante;
import sigpesq.model.ProjetoPesquisa;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        ProjetoPesquisaDAO dao = new ProjetoPesquisaDAO();

        System.out.println("=== SIGPesq - Teste de Conexao e Validacao ===");
        try {
            List<Participante> participantes = dao.listarParticipantesAtivos();
            System.out.println("\nParticipantes cadastrados (" + participantes.size() + "):");
            participantes.forEach(System.out::println);

            List<ProjetoPesquisa> projetos = dao.listarAtivosComCoordenador();
            System.out.println("\nProjetos ativos (" + projetos.size() + "):");
            projetos.forEach(System.out::println);

            // Teste rapido do Hibernate Validator (RN-10: termino anterior ao inicio)
            try {
                ProjetoPesquisa invalido = new ProjetoPesquisa(
                        "P999", 1, "Projeto Invalido", "Teste de validacao",
                        LocalDate.of(2026, 12, 1), LocalDate.of(2026, 1, 1),
                        "Em andamento", true
                );
                dao.inserir(invalido);
            } catch (IllegalArgumentException e) {
                System.out.println("\n[OK] Hibernate Validator bloqueou registro invalido -> " + e.getMessage());
            }

            // Inicia servidor HTTP leve na porta 8080 para integrar com index.html e app.js
            iniciarServidorHttp(dao, 8080);

        } catch (SQLException e) {
            System.err.println("Erro de banco de dados: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor HTTP: " + e.getMessage());
        }
    }

    private static void iniciarServidorHttp(ProjetoPesquisaDAO dao, int porta) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(porta), 0);

        // Endpoint JSON: GET /api/projetos
        server.createContext("/api/projetos", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    List<ProjetoPesquisa> projetos = dao.listarAtivosComCoordenador();
                    String json = converterProjetosParaJson(projetos);
                    enviarResposta(exchange, 200, "application/json; charset=UTF-8", json.getBytes(StandardCharsets.UTF_8));
                } catch (SQLException e) {
                    String erro = "{\"erro\": \"" + escaparJson(e.getMessage()) + "\"}";
                    enviarResposta(exchange, 500, "application/json; charset=UTF-8", erro.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                enviarResposta(exchange, 405, "text/plain; charset=UTF-8", "Metodo nao permitido".getBytes(StandardCharsets.UTF_8));
            }
        });

        // Servir arquivos estaticos de src/main/resources/public (index.html e app.js)
        server.createContext("/", exchange -> {
            String caminhoReq = exchange.getRequestURI().getPath();
            if ("/".equals(caminhoReq)) {
                caminhoReq = "/index.html";
            }
            Path arquivo = Paths.get("src/main/resources/public" + caminhoReq);
            if (Files.exists(arquivo) && !Files.isDirectory(arquivo)) {
                String contentType = caminhoReq.endsWith(".js") ? "application/javascript; charset=UTF-8" : "text/html; charset=UTF-8";
                enviarResposta(exchange, 200, contentType, Files.readAllBytes(arquivo));
            } else {
                enviarResposta(exchange, 404, "text/plain; charset=UTF-8", "Arquivo nao encontrado".getBytes(StandardCharsets.UTF_8));
            }
        });

        server.start();
        System.out.println("\nServidor HTTP rodando em: http://localhost:" + porta);
        System.out.println("API de projetos disponivel em: http://localhost:" + porta + "/api/projetos");
    }

    private static String converterProjetosParaJson(List<ProjetoPesquisa> projetos) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < projetos.size(); i++) {
            ProjetoPesquisa p = projetos.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"codigo\":\"").append(escaparJson(p.getCodigo())).append("\",")
              .append("\"idCoordenador\":").append(p.getIdCoordenador()).append(",")
              .append("\"nomeCoordenador\":\"").append(escaparJson(p.getNomeCoordenador())).append("\",")
              .append("\"titulo\":\"").append(escaparJson(p.getTitulo())).append("\",")
              .append("\"descricao\":\"").append(escaparJson(p.getDescricao())).append("\",")
              .append("\"dataInicio\":\"").append(p.getDataInicio()).append("\",")
              .append("\"dataTermino\":\"").append(p.getDataTermino()).append("\",")
              .append("\"situacao\":\"").append(escaparJson(p.getSituacao())).append("\"")
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escaparJson(String texto) {
        if (texto == null) return "";
        return texto.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private static void enviarResposta(HttpExchange exchange, int status, String contentType, byte[] corpo) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, corpo.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(corpo);
        }
    }
}