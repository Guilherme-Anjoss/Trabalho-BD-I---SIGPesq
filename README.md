# SIGPesq

Sistema de Gerenciamento de Projetos de Pesquisa Universitarios (Banco de Dados - UFES/CEUNES).

## Requisitos
- Docker e Docker Compose
- Java 21 e Maven (`sudo pacman -S jdk21-openjdk maven` no CachyOS/Arch)

## Como rodar
```bash
cp .env.example .env          # so na primeira vez
docker compose up -d          # sobe o MySQL e cria as tabelas
mvn compile exec:java         # sobe a aplicacao
```
Abra http://localhost:8080

Depois de alterar `sigpesq_schema.sql`, recrie o banco:
`docker compose down -v && docker compose up -d`

## Consultas SQL
`sigpesq_consultas.sql` tem as consultas dos relatorios e os testes das regras de negocio:
`docker exec -i sigpesq_db mysql -u root -p < sigpesq_consultas.sql`

## Telas do sistema
Menu lateral com 9 telas (front em HTML + CSS + JavaScript puro, sem framework):

| Tela | O que faz | Requisitos |
|---|---|---|
| Visão geral | Indicadores, financiamento por agência, projetos por situação, produções recentes | RF-20, RN-03 |
| Projetos | Cadastrar, buscar, filtrar por situação e alterar a situação | RF-01, RF-02, RF-05 |
| Participantes | Cadastrar (CPF com máscara e validação), listar com CPF mascarado (LGPD), inativar | RF-03, RF-24, RNF-03 |
| Vínculos | Vincular participante a projeto, filtrar por projeto/status, encerrar vínculo | RF-04, RF-06, RF-17 |
| Produções científicas | Registrar produção com autores em ordem (gravação em transação), filtrar por projeto/ano | RF-10 a RF-13, RF-21 |
| Agências de fomento | Cadastrar e ver total financiado por agência | RF-07, RF-09, RF-20 |
| Financiamentos | Cadastrar e acompanhar quanto já foi alocado e o saldo | RF-07, RF-19 |
| Alocações de recursos | Dividir um financiamento entre projetos (total/parcial) | RF-08, RF-22, RN-16 |
| Relatórios | As 4 consultas do roteiro, com botão "Ver SQL" para a apresentação | RF-05, RF-13, RF-14, RF-20 |

Toda regra violada aparece na tela com o código da regra (ex.: "RN-16: a soma das alocações ultrapassa o valor total").
A validação acontece em duas camadas: Bean Validation no Java e constraints/triggers no MySQL.

## API (JSON)
| Rota | GET | POST |
|---|---|---|
| `/api/projetos` | lista | cadastra |
| `/api/projetos/situacao` | | `{codigo, situacao}` |
| `/api/docentes` | id e nome dos docentes | |
| `/api/participantes` | lista (CPF mascarado) | cadastra |
| `/api/participantes/inativar` | | `{idParticipante}` |
| `/api/vinculos?projeto=P001` | lista | cadastra |
| `/api/vinculos/encerrar` | | `{idVinculo, dataSaida}` |
| `/api/producoes?ano=2026&projeto=P001` | lista com autores | cadastra com `autores: [ids em ordem]` |
| `/api/tipos-producao` | catálogo | |
| `/api/agencias` | lista com totais | cadastra |
| `/api/financiamentos` | lista com valor alocado | cadastra |
| `/api/alocacoes` | lista | cadastra |
| `/api/resumo` | números da visão geral | |
| `/api/relatorios/...` | `projetos-coordenadores?docente=1`, `producoes-tipo-ano`, `total-agencia`, `varios-projetos` | |
