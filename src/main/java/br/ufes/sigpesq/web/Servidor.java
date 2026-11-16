package br.ufes.sigpesq.web;

import br.ufes.sigpesq.dao.AgenciaFomentoDAO;
import br.ufes.sigpesq.dao.AlocacaoFinanciamentoDAO;
import br.ufes.sigpesq.dao.FinanciamentoDAO;
import br.ufes.sigpesq.dao.ParticipanteDAO;
import br.ufes.sigpesq.dao.ProducaoCientificaDAO;
import br.ufes.sigpesq.dao.ProjetoPesquisaDAO;
import br.ufes.sigpesq.dao.RelatorioDAO;
import br.ufes.sigpesq.dao.VinculoParticipacaoDAO;
import br.ufes.sigpesq.model.AgenciaFomento;
import br.ufes.sigpesq.model.AlocacaoFinanciamento;
import br.ufes.sigpesq.model.Financiamento;
import br.ufes.sigpesq.model.Participante;
import br.ufes.sigpesq.model.ProducaoCientifica;
import br.ufes.sigpesq.model.ProjetoPesquisa;
import br.ufes.sigpesq.model.VinculoParticipacao;
import br.ufes.sigpesq.validation.ValidadorEntidade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;

import java.util.Map;
import java.util.Set;

/**
 * Servidor web simples (JDK puro): serve a pasta public/ e a API JSON em /api.
 *
 * Cada cadastro passa por duas camadas de validacao:
 *   1a camada: Bean Validation nas classes do model (mensagem rapida e amigavel);
 *   2a camada: constraints e triggers do MySQL (garantia final, vale ate para SQL direto).
 */
public class Servidor {

    private record Opcao(int id, String nome) {}

    /** Uma acao da API: recebe a requisicao e devolve o objeto que vira JSON. */
    @FunctionalInterface
    private interface Acao {
        Object executar(HttpExchange ex) throws Exception;
    }

    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class,
            (JsonSerializer<LocalDate>) (data, tipo, ctx) -> new JsonPrimitive(data.toString()))
        .registerTypeAdapter(LocalDate.class,
            (JsonDeserializer<LocalDate>) (json, tipo, ctx) -> {
                String texto = json.getAsString();
                if (texto == null || texto.isBlank()) return null;
                try {
                    return LocalDate.parse(texto);
                } catch (DateTimeParseException e) {
                    throw new JsonParseException("Data inválida: " + texto);
                }
            })
        .create();

    private static final Set<String> SITUACOES = Set.of("Em andamento", "Concluido", "Cancelado");

    private static final ProjetoPesquisaDAO projetoDAO = new ProjetoPesquisaDAO();
    private static final ParticipanteDAO participanteDAO = new ParticipanteDAO();
    private static final VinculoParticipacaoDAO vinculoDAO = new VinculoParticipacaoDAO();
    private static final AgenciaFomentoDAO agenciaDAO = new AgenciaFomentoDAO();
    private static final FinanciamentoDAO financiamentoDAO = new FinanciamentoDAO();
    private static final AlocacaoFinanciamentoDAO alocacaoDAO = new AlocacaoFinanciamentoDAO();
    private static final ProducaoCientificaDAO producaoDAO = new ProducaoCientificaDAO();
    private static final RelatorioDAO relatorioDAO = new RelatorioDAO();

    public static void iniciar(int porta) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(porta), 0);

        // ---------- Projetos (RF-01, RF-02, RF-05) ----------
        servidor.createContext("/api/projetos", rota(
            ex -> projetoDAO.listar(),
            ex -> {
                ProjetoPesquisa p = ler(ex, ProjetoPesquisa.class);
                p.normalizar();
                ValidadorEntidade.validar(p);
                projetoDAO.inserir(p);
                return p;
            }));
        servidor.createContext("/api/projetos/situacao", rota(null, ex -> {
            JsonObject corpo = ler(ex, JsonObject.class);
            String codigo = texto(corpo, "codigo");
            String situacao = texto(corpo, "situacao");
            if (codigo == null || situacao == null) throw new IllegalArgumentException("Informe o projeto e a situação.");
            if (!SITUACOES.contains(situacao)) {
                throw new IllegalArgumentException("RN-03: A situação deve ser Em andamento, Concluido ou Cancelado.");
            }
            projetoDAO.atualizarSituacao(codigo, situacao);
            return Map.of("ok", true);
        }));

        // ---------- Participantes (RF-03) ----------
        servidor.createContext("/api/docentes", rota(ex ->
            participanteDAO.listarDocentesAtivos().stream()
                .map(p -> new Opcao(p.getIdParticipante(), p.getNome()))   // sem CPF/e-mail (LGPD)
                .toList(), null));
        servidor.createContext("/api/participantes", rota(
            ex -> participanteDAO.listar(),
            ex -> {
                Participante p = ler(ex, Participante.class);
                if (p.getNome() != null) p.setNome(p.getNome().trim());
                if (p.getCpf() != null) p.setCpf(p.getCpf().replaceAll("\\D", ""));   // aceita CPF com pontuacao
                if (p.getEmailInstitucional() != null) p.setEmailInstitucional(p.getEmailInstitucional().trim().toLowerCase());
                ValidadorEntidade.validar(p);
                participanteDAO.inserir(p);
                return p;
            }));
        servidor.createContext("/api/participantes/inativar", rota(null, ex -> {
            participanteDAO.inativar(inteiro(ler(ex, JsonObject.class), "idParticipante"));
            return Map.of("ok", true);
        }));

        // ---------- Vinculos (RF-04, RF-06, RF-17) ----------
        servidor.createContext("/api/vinculos", rota(
            ex -> vinculoDAO.listar(parametro(ex, "projeto")),
            ex -> {
                VinculoParticipacao v = ler(ex, VinculoParticipacao.class);
                v.normalizar();
                ValidadorEntidade.validar(v);
                vinculoDAO.inserir(v);
                return v;
            }));
        servidor.createContext("/api/vinculos/encerrar", rota(null, ex -> {
            JsonObject corpo = ler(ex, JsonObject.class);
            String data = texto(corpo, "dataSaida");
            if (data == null) throw new IllegalArgumentException("Informe a data de saída.");
            vinculoDAO.encerrar(inteiro(corpo, "idVinculo"), LocalDate.parse(data));
            return Map.of("ok", true);
        }));

        // ---------- Producoes (RF-10 a RF-13, RF-21) ----------
        servidor.createContext("/api/tipos-producao", rota(ex -> producaoDAO.listarTipos(), null));
        servidor.createContext("/api/producoes", rota(
            ex -> {
                String ano = parametro(ex, "ano");
                return producaoDAO.listar(ano != null ? Integer.valueOf(ano) : null, parametro(ex, "projeto"));
            },
            ex -> {
                ProducaoCientifica pc = ler(ex, ProducaoCientifica.class);
                pc.normalizar();
                ValidadorEntidade.validar(pc);
                producaoDAO.inserir(pc);
                return pc;
            }));

        // ---------- Fomento (RF-07, RF-08, RF-09, RF-20, RF-22) ----------
        servidor.createContext("/api/agencias", rota(
            ex -> agenciaDAO.listar(),
            ex -> {
                AgenciaFomento a = ler(ex, AgenciaFomento.class);
                a.normalizar();
                ValidadorEntidade.validar(a);
                agenciaDAO.inserir(a);
                return a;
            }));
        servidor.createContext("/api/financiamentos", rota(
            ex -> financiamentoDAO.listar(),
            ex -> {
                Financiamento f = ler(ex, Financiamento.class);
                f.normalizar();
                ValidadorEntidade.validar(f);
                financiamentoDAO.inserir(f);
                return f;
            }));
        servidor.createContext("/api/alocacoes", rota(
            ex -> alocacaoDAO.listar(),
            ex -> {
                AlocacaoFinanciamento a = ler(ex, AlocacaoFinanciamento.class);
                ValidadorEntidade.validar(a);
                alocacaoDAO.inserir(a);
                return a;
            }));

        // ---------- Relatorios (consultas do roteiro) ----------
        servidor.createContext("/api/resumo", rota(ex -> relatorioDAO.resumo(), null));
        servidor.createContext("/api/relatorios/projetos-coordenadores", rota(ex -> {
            String docente = parametro(ex, "docente");
            return relatorioDAO.projetosComCoordenadores(docente != null ? Integer.valueOf(docente) : null);
        }, null));
        servidor.createContext("/api/relatorios/producoes-tipo-ano", rota(ex -> relatorioDAO.producoesPorTipoEAno(), null));
        servidor.createContext("/api/relatorios/total-agencia", rota(ex -> relatorioDAO.totalPorAgencia(), null));
        servidor.createContext("/api/relatorios/varios-projetos", rota(ex -> relatorioDAO.participantesEmVariosProjetos(), null));

        servidor.createContext("/", Servidor::estatico);
        servidor.start();
    }

    // =====================================================================
    // Infraestrutura das rotas
    // =====================================================================

    /** Monta uma rota com GET e/ou POST e o tratamento de erros padrao (400 regra violada, 500 erro interno). */
    private static HttpHandler rota(Acao get, Acao post) {
        return ex -> {
            try {
                String metodo = ex.getRequestMethod();
                if (metodo.equals("GET") && get != null) {
                    responder(ex, 200, GSON.toJson(get.executar(ex)));
                } else if (metodo.equals("POST") && post != null) {
                    responder(ex, 201, GSON.toJson(post.executar(ex)));
                } else {
                    responder(ex, 405, erro("Método não permitido."));
                }
            } catch (IllegalArgumentException | JsonParseException | DateTimeParseException e) {
                responder(ex, 400, erro(e.getMessage()));            // NumberFormatException tambem cai aqui
            } catch (Exception e) {
                e.printStackTrace();
                responder(ex, 500, erro("Erro interno do servidor."));
            }
        };
    }

    /** Le o corpo JSON da requisicao e converte para a classe pedida. */
    private static <T> T ler(HttpExchange ex, Class<T> classe) throws IOException {
        String corpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        T objeto = GSON.fromJson(corpo, classe);
        if (objeto == null) throw new IllegalArgumentException("Corpo da requisição vazio.");
        return objeto;
    }

    /** Le um parametro da URL (?projeto=P001). Devolve null se nao vier ou vier vazio. */
    private static String parametro(HttpExchange ex, String nome) {
        String query = ex.getRequestURI().getRawQuery();
        if (query == null) return null;
        Map<String, String> valores = new HashMap<>();
        for (String par : query.split("&")) {
            String[] partes = par.split("=", 2);
            if (partes.length == 2) {
                valores.put(URLDecoder.decode(partes[0], StandardCharsets.UTF_8),
                            URLDecoder.decode(partes[1], StandardCharsets.UTF_8));
            }
        }
        String valor = valores.get(nome);
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static String texto(JsonObject json, String campo) {
        return json.has(campo) && !json.get(campo).isJsonNull() ? json.get(campo).getAsString().trim() : null;
    }

    private static int inteiro(JsonObject json, String campo) {
        String valor = texto(json, campo);
        if (valor == null) throw new IllegalArgumentException("Dados incompletos. Atualize a página e tente novamente.");
        return Integer.parseInt(valor);
    }

    // Serve os arquivos de src/main/resources/public (index.html, app.js, estilo.css...)
    private static void estatico(HttpExchange ex) throws IOException {
        String caminho = ex.getRequestURI().getPath();
        if (caminho.equals("/")) caminho = "/index.html";
        if (caminho.contains("..")) {
            responder(ex, 400, erro("Caminho inválido."));
            return;
        }
        try (InputStream in = Servidor.class.getResourceAsStream("/public" + caminho)) {
            if (in == null) {
                responder(ex, 404, erro("Arquivo não encontrado."));
                return;
            }
            byte[] dados = in.readAllBytes();
            ex.getResponseHeaders().set("Content-Type", tipoDoArquivo(caminho));
            ex.sendResponseHeaders(200, dados.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(dados); }
        }
    }

    private static String tipoDoArquivo(String caminho) {
        if (caminho.endsWith(".html")) return "text/html; charset=utf-8";
        if (caminho.endsWith(".js"))   return "text/javascript; charset=utf-8";
        if (caminho.endsWith(".css"))  return "text/css; charset=utf-8";
        if (caminho.endsWith(".svg"))  return "image/svg+xml";
        return "application/octet-stream";
    }

    private static String erro(String mensagem) {
        return GSON.toJson(Map.of("erro", mensagem == null ? "Erro inesperado." : mensagem));
    }

    private static void responder(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

}
