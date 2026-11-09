-- =====================================================================
-- SIGPesq - Sistema de Gerenciamento de Projetos de Pesquisa Universitarios
-- Script de Implementacao Fisica (DDL + DML de carga inicial)
-- SGBD alvo: MySQL 8.4 LTS
--
-- Seguinte gurizada, ste script traduz o Modelo Logico do documento em tabelas MySQL. 
-- As regras de negocio (RN) que podem ser expressas como restricoes declarativas viram PRIMARY KEY / FOREIGN KEY / UNIQUE / CHECK; 
-- As que exigem comparar valores entre linhas ou entre tabelas (RN-02, RN-16, RN-17, RN-18, RN-19) viram TRIGGERS.
-- =====================================================================

DROP DATABASE IF EXISTS sigpesq;
CREATE DATABASE sigpesq
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE sigpesq;

-- =====================================================================
-- 1. TABELAS SEM DEPENDENCIAS (entidades "folha")
-- =====================================================================

-- ---------------------------------------------------------------------
-- AGENCIA_FOMENTO
-- ---------------------------------------------------------------------
CREATE TABLE agencia_fomento (
    id_agencia      INT AUTO_INCREMENT PRIMARY KEY,
    nome            VARCHAR(150)    NOT NULL,
    sigla           VARCHAR(20)     NOT NULL,
    ativo           BOOLEAN         NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_agencia_sigla UNIQUE (sigla)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- TIPO_PRODUCAO  (catalogo extensivel -- RN-15: lista aberta, nao fechada)
-- ---------------------------------------------------------------------
CREATE TABLE tipo_producao (
    id_tipo_producao INT AUTO_INCREMENT PRIMARY KEY,
    nome            VARCHAR(100)    NOT NULL,
    ativo           BOOLEAN         NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_tipo_producao_nome UNIQUE (nome)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- PARTICIPANTE  (RF-03 / RF-18; RN-05, RN-09, RN-14)
-- ---------------------------------------------------------------------
CREATE TABLE participante (
    id_participante INT AUTO_INCREMENT PRIMARY KEY,
    tipo            VARCHAR(20)     NOT NULL,
    nome            VARCHAR(150)    NOT NULL,
    cpf             CHAR(11)        NOT NULL,
    email_institucional VARCHAR(120) NOT NULL,
    ativo           BOOLEAN         NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_participante_cpf   UNIQUE (cpf),            -- RN-09
    CONSTRAINT uq_participante_email UNIQUE (email_institucional), -- RN-14
    CONSTRAINT ck_participante_tipo CHECK (tipo IN ('Docente', 'Discente', 'Tecnico'))
) ENGINE = InnoDB;

-- =====================================================================
-- 2. TABELAS COM DEPENDENCIA DE 1 NIVEL
-- =====================================================================

-- ---------------------------------------------------------------------
-- FINANCIAMENTO  (RF-07 / RF-19; RN-08, RN-12)
-- ---------------------------------------------------------------------
CREATE TABLE financiamento (
    id_financiamento INT AUTO_INCREMENT PRIMARY KEY,
    id_agencia      INT             NOT NULL,
    tipo_fomento    VARCHAR(50)     NOT NULL,
    inicio_vigencia DATE            NOT NULL,
    fim_vigencia    DATE            NOT NULL,
    valor_total     DECIMAL(15,2)   NOT NULL,
    ativo           BOOLEAN         NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_financiamento_agencia FOREIGN KEY (id_agencia)
        REFERENCES agencia_fomento (id_agencia)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_financiamento_vigencia CHECK (fim_vigencia > inicio_vigencia), -- RN-12
    CONSTRAINT ck_financiamento_valor CHECK (valor_total > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- PROJETO_PESQUISA  (RF-01 / RF-02 / RF-15; RN-01, RN-02*, RN-03, RN-04, RN-10)
-- * RN-02 (coordenador deve ser Docente) eh garantida por trigger, Secao 5.
-- ---------------------------------------------------------------------
CREATE TABLE projeto_pesquisa (
    codigo          VARCHAR(20)     PRIMARY KEY,              -- RN-04 (unicidade via PK)
    id_coordenador  INT             NOT NULL,
    titulo          VARCHAR(200)    NOT NULL,
    descricao       TEXT,
    data_inicio     DATE            NOT NULL,
    data_termino    DATE            NOT NULL,
    situacao        VARCHAR(20)     NOT NULL DEFAULT 'Em andamento',
    ativo           BOOLEAN         NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_projeto_coordenador FOREIGN KEY (id_coordenador)
        REFERENCES participante (id_participante)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_projeto_situacao CHECK (situacao IN ('Em andamento', 'Concluido', 'Cancelado')), -- RN-03
    CONSTRAINT ck_projeto_datas CHECK (data_termino > data_inicio) -- RN-10
) ENGINE = InnoDB;

-- =====================================================================
-- 3. TABELAS COM DEPENDENCIA DE 2 NIVEIS (associativas)
-- =====================================================================

-- ---------------------------------------------------------------------
-- PRODUCAO_CIENTIFICA  (RF-10 / RF-23; RN-18*)
-- ---------------------------------------------------------------------
CREATE TABLE producao_cientifica (
    id_producao     INT AUTO_INCREMENT PRIMARY KEY,
    codigo_projeto  VARCHAR(20)     NOT NULL,
    id_tipo_producao INT            NOT NULL,
    titulo          VARCHAR(200)    NOT NULL,
    ano_publicacao  INT             NOT NULL,
    meio_divulgacao VARCHAR(150),
    CONSTRAINT fk_producao_projeto FOREIGN KEY (codigo_projeto)
        REFERENCES projeto_pesquisa (codigo)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_producao_tipo FOREIGN KEY (id_tipo_producao)
        REFERENCES tipo_producao (id_tipo_producao)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_producao_ano CHECK (ano_publicacao BETWEEN 1900 AND 2100)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- VINCULO_PARTICIPACAO  (RF-04 / RF-17; RN-07, RN-11, RN-17*, RN-19*)
-- ---------------------------------------------------------------------
CREATE TABLE vinculo_participacao (
    id_vinculo      INT AUTO_INCREMENT PRIMARY KEY,
    id_participante INT             NOT NULL,
    codigo_projeto  VARCHAR(20)     NOT NULL,
    funcao          VARCHAR(40)     NOT NULL,
    data_entrada    DATE            NOT NULL,
    data_saida      DATE            NULL,
    CONSTRAINT fk_vinculo_participante FOREIGN KEY (id_participante)
        REFERENCES participante (id_participante)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_vinculo_projeto FOREIGN KEY (codigo_projeto)
        REFERENCES projeto_pesquisa (codigo)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_vinculo_datas CHECK (data_saida IS NULL OR data_saida > data_entrada) -- RN-11
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- ALOCACAO_FINANCIAMENTO  (RF-08 / RF-22; RN-08, RN-13, RN-16*)
-- ---------------------------------------------------------------------
CREATE TABLE alocacao_financiamento (
    id_alocacao     INT AUTO_INCREMENT PRIMARY KEY,
    codigo_projeto  VARCHAR(20)     NOT NULL,
    id_financiamento INT            NOT NULL,
    modalidade      VARCHAR(20)     NOT NULL,
    valor_destinado DECIMAL(15,2)   NOT NULL,
    CONSTRAINT fk_alocacao_projeto FOREIGN KEY (codigo_projeto)
        REFERENCES projeto_pesquisa (codigo)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_alocacao_financiamento FOREIGN KEY (id_financiamento)
        REFERENCES financiamento (id_financiamento)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uq_alocacao_projeto_financiamento UNIQUE (codigo_projeto, id_financiamento),
    CONSTRAINT ck_alocacao_modalidade CHECK (modalidade IN ('Total', 'Parcial')),
    CONSTRAINT ck_alocacao_valor CHECK (valor_destinado > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- AUTORIA  (RF-11; RN-06 garantida pela propria FK id_participante)
-- ---------------------------------------------------------------------
CREATE TABLE autoria (
    id_producao     INT             NOT NULL,
    id_participante INT             NOT NULL,
    ordem_autoria   INT             NULL,
    PRIMARY KEY (id_producao, id_participante),
    CONSTRAINT fk_autoria_producao FOREIGN KEY (id_producao)
        REFERENCES producao_cientifica (id_producao)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_autoria_participante FOREIGN KEY (id_participante)
        REFERENCES participante (id_participante)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB;

-- =====================================================================
-- 4. INDICES ADICIONAIS (apoio a consultas do Capitulo 2 - UC-05, UC-08,
--    UC-10, UC-13: relatorios por status, por ano e por tipo de producao)
-- =====================================================================
CREATE INDEX idx_projeto_situacao   ON projeto_pesquisa (situacao);
CREATE INDEX idx_producao_ano       ON producao_cientifica (ano_publicacao);
CREATE INDEX idx_vinculo_participante ON vinculo_participacao (id_participante);

-- =====================================================================
-- 5. TRIGGERS -- regras de negocio que comparam valores entre linhas
--    ou entre tabelas (nao expressaveis apenas com CHECK/FK/UNIQUE)
-- =====================================================================
DELIMITER $$

-- RN-02: o coordenador principal de um projeto deve ser do tipo Docente
CREATE TRIGGER trg_projeto_coordenador_docente_ins
BEFORE INSERT ON projeto_pesquisa
FOR EACH ROW
BEGIN
    DECLARE v_tipo VARCHAR(20);
    SELECT tipo INTO v_tipo FROM participante WHERE id_participante = NEW.id_coordenador;
    IF v_tipo IS NULL OR v_tipo <> 'Docente' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN-02: o coordenador principal do projeto deve ser um participante do tipo Docente.';
    END IF;
END$$

CREATE TRIGGER trg_projeto_coordenador_docente_upd
BEFORE UPDATE ON projeto_pesquisa
FOR EACH ROW
BEGIN
    DECLARE v_tipo VARCHAR(20);
    SELECT tipo INTO v_tipo FROM participante WHERE id_participante = NEW.id_coordenador;
    IF v_tipo IS NULL OR v_tipo <> 'Docente' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN-02: o coordenador principal do projeto deve ser um participante do tipo Docente.';
    END IF;
END$$

-- RN-17: o periodo do vinculo deve estar contido na vigencia do projeto
CREATE TRIGGER trg_vinculo_periodo_ins
BEFORE INSERT ON vinculo_participacao
FOR EACH ROW
BEGIN
    DECLARE v_inicio DATE;
    DECLARE v_termino DATE;
    SELECT data_inicio, data_termino INTO v_inicio, v_termino
        FROM projeto_pesquisa WHERE codigo = NEW.codigo_projeto;
    IF NEW.data_entrada < v_inicio THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN-17: a data de entrada do vinculo nao pode ser anterior ao inicio do projeto.';
    END IF;
    IF NEW.data_saida IS NOT NULL AND NEW.data_saida > v_termino THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN-17: a data de saida do vinculo nao pode ser posterior ao termino do projeto.';
    END IF;
END$$

CREATE TRIGGER trg_vinculo_periodo_upd
BEFORE UPDATE ON vinculo_participacao
FOR EACH ROW
BEGIN
    DECLARE v_inicio DATE;
    DECLARE v_termino DATE;
    SELECT data_inicio, data_termino INTO v_inicio, v_termino
        FROM projeto_pesquisa WHERE codigo = NEW.codigo_projeto;
    IF NEW.data_entrada < v_inicio THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN-17: a data de entrada do vinculo nao pode ser anterior ao inicio do projeto.';
    END IF;
    IF NEW.data_saida IS NOT NULL AND NEW.data_saida > v_termino THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN-17: a data de saida do vinculo nao pode ser posterior ao termino do projeto.';
    END IF;
END$$

-- RN-19: um participante nao pode ter dois vinculos ATIVOS (data_saida
-- NULL) com a mesma funcao no mesmo projeto simultaneamente
CREATE TRIGGER trg_vinculo_sem_duplicidade_ins
BEFORE INSERT ON vinculo_participacao
FOR EACH ROW
BEGIN
    DECLARE v_existe INT;
    IF NEW.data_saida IS NULL THEN
        SELECT COUNT(*) INTO v_existe FROM vinculo_participacao
            WHERE id_participante = NEW.id_participante
              AND codigo_projeto = NEW.codigo_projeto
              AND funcao = NEW.funcao
              AND data_saida IS NULL;
        IF v_existe > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'RN-19: ja existe um vinculo ativo do participante nessa funcao, nesse projeto.';
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_vinculo_sem_duplicidade_upd
BEFORE UPDATE ON vinculo_participacao
FOR EACH ROW
BEGIN
    DECLARE v_existe INT;
    IF NEW.data_saida IS NULL THEN
        SELECT COUNT(*) INTO v_existe FROM vinculo_participacao
            WHERE id_participante = NEW.id_participante
              AND codigo_projeto = NEW.codigo_projeto
              AND funcao = NEW.funcao
              AND data_saida IS NULL
              AND id_vinculo <> NEW.id_vinculo;
        IF v_existe > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'RN-19: ja existe um vinculo ativo do participante nessa funcao, nesse projeto.';
        END IF;
    END IF;
END$$

-- RN-18: o ano de publicacao da producao nao pode ser anterior ao ano
-- de inicio do projeto que a gerou
CREATE TRIGGER trg_producao_ano_ins
BEFORE INSERT ON producao_cientifica
FOR EACH ROW
BEGIN
    DECLARE v_ano_inicio INT;
    SELECT YEAR(data_inicio) INTO v_ano_inicio FROM projeto_pesquisa WHERE codigo = NEW.codigo_projeto;
    IF NEW.ano_publicacao < v_ano_inicio THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN-18: o ano de publicacao nao pode ser anterior ao ano de inicio do projeto.';
    END IF;
END$$

CREATE TRIGGER trg_producao_ano_upd
BEFORE UPDATE ON producao_cientifica
FOR EACH ROW
BEGIN
    DECLARE v_ano_inicio INT;
    SELECT YEAR(data_inicio) INTO v_ano_inicio FROM projeto_pesquisa WHERE codigo = NEW.codigo_projeto;
    IF NEW.ano_publicacao < v_ano_inicio THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN-18: o ano de publicacao nao pode ser anterior ao ano de inicio do projeto.';
    END IF;
END$$

-- RN-16: a soma dos valores destinados a um mesmo financiamento nao
-- pode ultrapassar o valor_total desse financiamento
CREATE TRIGGER trg_alocacao_limite_ins
BEFORE INSERT ON alocacao_financiamento
FOR EACH ROW
BEGIN
    DECLARE v_total DECIMAL(15,2);
    DECLARE v_alocado DECIMAL(15,2);
    SELECT valor_total INTO v_total FROM financiamento WHERE id_financiamento = NEW.id_financiamento;
    SELECT COALESCE(SUM(valor_destinado), 0) INTO v_alocado FROM alocacao_financiamento
        WHERE id_financiamento = NEW.id_financiamento;
    IF (v_alocado + NEW.valor_destinado) > v_total THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN-16: a soma das alocacoes ultrapassa o valor total do financiamento.';
    END IF;
END$$

CREATE TRIGGER trg_alocacao_limite_upd
BEFORE UPDATE ON alocacao_financiamento
FOR EACH ROW
BEGIN
    DECLARE v_total DECIMAL(15,2);
    DECLARE v_alocado DECIMAL(15,2);
    SELECT valor_total INTO v_total FROM financiamento WHERE id_financiamento = NEW.id_financiamento;
    SELECT COALESCE(SUM(valor_destinado), 0) INTO v_alocado FROM alocacao_financiamento
        WHERE id_financiamento = NEW.id_financiamento AND id_alocacao <> NEW.id_alocacao;
    IF (v_alocado + NEW.valor_destinado) > v_total THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN-16: a soma das alocacoes ultrapassa o valor total do financiamento.';
    END IF;
END$$

DELIMITER ;

-- =====================================================================
-- 6. CARGA INICIAL DE DADOS (DML de exemplo)
-- =====================================================================

-- ---- Agencias de fomento -------------------------------------------
INSERT INTO agencia_fomento (nome, sigla, ativo) VALUES
    ('Conselho Nacional de Desenvolvimento Cientifico e Tecnologico', 'CNPq', TRUE),
    ('Fundacao de Amparo a Pesquisa e Inovacao do Espirito Santo',     'FAPES', TRUE),
    ('Coordenacao de Aperfeicoamento de Pessoal de Nivel Superior',    'CAPES', TRUE);

-- ---- Tipos de producao (catalogo extensivel) ------------------------
INSERT INTO tipo_producao (nome, ativo) VALUES
    ('Artigo', TRUE),
    ('Capitulo de Livro', TRUE),
    ('Software', TRUE),
    ('Relatorio Tecnico', TRUE);

-- ---- Participantes ----------------------------------------------------
INSERT INTO participante (tipo, nome, cpf, email_institucional, ativo) VALUES
    ('Docente',  'Ana Beatriz Souza',     '11122233344', 'ana.souza@ufnes.edu.br',     TRUE),
    ('Docente',  'Carlos Eduardo Lima',   '22233344455', 'carlos.lima@ufnes.edu.br',   TRUE),
    ('Discente', 'Fernanda Dias Rocha',   '33344455566', 'fernanda.rocha@ufnes.edu.br', TRUE),
    ('Discente', 'Gabriel Martins Alves', '44455566677', 'gabriel.alves@ufnes.edu.br', TRUE),
    ('Tecnico',  'Helena Castro Pires',   '55566677788', 'helena.pires@ufnes.edu.br',  TRUE);

-- ---- Financiamentos ---------------------------------------------------
INSERT INTO financiamento (id_agencia, tipo_fomento, inicio_vigencia, fim_vigencia, valor_total, ativo) VALUES
    (1, 'Bolsa',    '2025-01-01', '2026-12-31', 50000.00, TRUE),  -- CNPq
    (2, 'Auxilio',  '2025-06-01', '2026-05-31', 30000.00, TRUE);  -- FAPES

-- ---- Projetos de pesquisa ----------------------------------------------
-- id_participante 1 = Ana Beatriz (Docente), coordena 2 projetos (RN-05)
-- id_participante 2 = Carlos Eduardo (Docente), coordena 1 projeto
INSERT INTO projeto_pesquisa (codigo, id_coordenador, titulo, descricao, data_inicio, data_termino, situacao, ativo) VALUES
    ('P001', 1, 'Monitoramento de Qualidade da Agua em Bacias Urbanas',
        'Avaliacao de parametros fisico-quimicos em cursos de agua da regiao metropolitana.',
        '2025-02-01', '2026-12-31', 'Em andamento', TRUE),
    ('P002', 1, 'Inteligencia Artificial Aplicada a Agricultura de Precisao',
        'Desenvolvimento de modelos preditivos para apoio a decisao no manejo agricola.',
        '2025-03-01', '2026-02-28', 'Concluido', TRUE),
    ('P003', 2, 'Educacao Ambiental em Escolas Publicas',
        'Diagnostico e proposta de intervencoes de educacao ambiental na rede municipal.',
        '2025-05-01', '2026-10-31', 'Em andamento', TRUE);

-- ---- Vinculos de participacao -------------------------------------------
INSERT INTO vinculo_participacao (id_participante, codigo_projeto, funcao, data_entrada, data_saida) VALUES
    (1, 'P001', 'Coordenador', '2025-02-01', NULL),
    (1, 'P002', 'Coordenador', '2025-03-01', NULL),
    (2, 'P003', 'Coordenador', '2025-05-01', NULL),
    (3, 'P001', 'Bolsista',    '2025-02-15', NULL),   -- Fernanda em P001...
    (3, 'P003', 'Colaborador', '2025-06-01', NULL),   -- ...e tambem em P003 (RF-14: atua em >1 projeto)
    (4, 'P001', 'Bolsista',    '2025-03-01', NULL),   -- Gabriel
    (4, 'P002', 'Bolsista',    '2025-04-01', NULL),   -- Gabriel tambem em P002 (autor das producoes de IA)
    (5, 'P002', 'Colaborador', '2025-03-15', '2025-12-31'); -- Helena, vinculo ja encerrado

-- ---- Alocacoes de financiamento ------------------------------------------
-- O financiamento 1 (CNPq, R$ 50.000,00) eh dividido entre P001 e P002:
-- 20.000,00 + 30.000,00 = 50.000,00 (exatamente o limite -- RN-16)
INSERT INTO alocacao_financiamento (codigo_projeto, id_financiamento, modalidade, valor_destinado) VALUES
    ('P001', 1, 'Parcial', 20000.00),
    ('P002', 1, 'Parcial', 30000.00),
    ('P003', 2, 'Total',   30000.00);

-- ---- Producoes cientificas ------------------------------------------------
INSERT INTO producao_cientifica (codigo_projeto, id_tipo_producao, titulo, ano_publicacao, meio_divulgacao) VALUES
    ('P001', 1, 'Qualidade da agua em bacias urbanas: um estudo de caso', 2025, 'Revista Brasileira de Recursos Hidricos'),
    ('P002', 3, 'AgroIA: sistema de apoio a decisao agricola',            2026, 'Repositorio institucional UFNES'),
    ('P002', 1, 'Redes neurais aplicadas a previsao de safra',            2026, 'Congresso Brasileiro de Agroinformatica'),
    ('P003', 4, 'Diagnostico de educacao ambiental na rede publica',      2026, 'Relatorio interno UFNES');

-- ---- Autoria das producoes --------------------------------------------
INSERT INTO autoria (id_producao, id_participante, ordem_autoria) VALUES
    (1, 1, 1), (1, 3, 2),          -- Ana Beatriz e Fernanda
    (2, 1, 1), (2, 4, 2),          -- Ana Beatriz e Gabriel
    (3, 4, 1), (3, 1, 2),          -- Gabriel e Ana Beatriz
    (4, 2, 1), (4, 3, 2);          -- Carlos Eduardo e Fernanda

-- =====================================================================
-- Fim do script. Para validar rapidamente apos a execucao:
--   SELECT codigo, titulo, situacao FROM projeto_pesquisa;
--   SELECT COUNT(*) FROM participante;
-- =====================================================================
