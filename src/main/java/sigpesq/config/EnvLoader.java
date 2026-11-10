package sigpesq.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvLoader {

    private static final Map<String, String> VARIAVEIS = new HashMap<>();

    static {
        carregarArquivoEnv(Paths.get(".env"));
    }

    private static void carregarArquivoEnv(Path caminho) {
        if (!Files.exists(caminho)) {
            throw new RuntimeException("Arquivo .env nao encontrado em: " + caminho.toAbsolutePath());
        }

        try {
            List<String> linhas = Files.readAllLines(caminho);
            for (String linha : linhas) {
                String limpa = linha.trim();
                if (limpa.isEmpty() || limpa.startsWith("#") || !limpa.contains("=")) {
                    continue;
                }

                String[] partes = limpa.split("=", 2);
                String chave = partes[0].trim();
                String valor = partes[1].trim();

                // Remove aspas simples ou duplas caso existam ao redor do valor
                if ((valor.startsWith("\"") && valor.endsWith("\"")) ||
                    (valor.startsWith("'") && valor.endsWith("'"))) {
                    valor = valor.substring(1, valor.length() - 1);
                }

                VARIAVEIS.put(chave, valor);
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler o arquivo .env: " + e.getMessage(), e);
        }
    }

    public static String get(String chave) {
        String valor = VARIAVEIS.get(chave);
        if (valor == null) {
            throw new IllegalArgumentException("Variavel '" + chave + "' nao definida no arquivo .env");
        }
        return valor;
    }

    public static String getOrDefault(String chave, String valorPadrao) {
        return VARIAVEIS.getOrDefault(chave, valorPadrao);
    }
}