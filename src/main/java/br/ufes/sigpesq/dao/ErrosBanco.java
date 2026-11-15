package br.ufes.sigpesq.dao;

import java.sql.SQLException;

/**
 * Converte os erros do MySQL em mensagens que o usuario entende.
 * Assim a tela mostra a regra violada (RN-xx) em vez de um erro tecnico.
 */
public final class ErrosBanco {

    private ErrosBanco() {}

    /**
     * @param e         erro devolvido pelo JDBC
     * @param duplicado mensagem usada quando uma chave UNIQUE/PRIMARY KEY se repete (erro 1062)
     */
    public static RuntimeException traduzir(SQLException e, String duplicado) {
        if ("45000".equals(e.getSQLState())) {              // SIGNAL disparado pelos triggers
            return new IllegalArgumentException(e.getMessage());
        }
        return switch (e.getErrorCode()) {
            case 1062 -> new IllegalArgumentException(duplicado);
            case 1452 -> new IllegalArgumentException("Um dos itens selecionados não foi encontrado. Atualize a página e tente novamente.");
            case 1451 -> new IllegalArgumentException("Este registro está sendo usado em outros cadastros e não pode ser removido.");
            case 3819 -> new IllegalArgumentException("Algum valor informado não é permitido. Confira os campos e tente novamente.");
            case 1048 -> new IllegalArgumentException("Um campo obrigatório não foi informado.");
            default -> new RuntimeException("Erro ao acessar o banco", e);
        };
    }
}
