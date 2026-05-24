# API de Gestão de Contas a Pagar

API REST desenvolvida em Java com Spring Boot para gestão de contas a pagar.

O projeto contempla:

- autenticação JWT
- CRUD completo de contas
- relacionamento obrigatório com fornecedores
- paginação e filtros
- relatório de valores pagos por período
- importação assíncrona de CSV utilizando RabbitMQ
- documentação Swagger/OpenAPI
- collection Postman pronta para testes

---

# Tecnologias utilizadas

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security
- JWT
- PostgreSQL
- Flyway
- RabbitMQ
- Docker
- Docker Compose
- Bean Validation
- Swagger/OpenAPI
- JUnit 5
- Mockito

---

# Infraestrutura Docker

O projeto utiliza Docker Compose para subir toda a infraestrutura necessária localmente.

Containers utilizados:

| Container | Função | Porta |
|---|---|---|
| app | API Spring Boot | 8080 |
| postgres | Banco PostgreSQL | 5432 |
| rabbitmq | Broker de mensageria | 5672 |
| rabbitmq-management | Interface RabbitMQ | 15672 |

---

# Requisitos

Para executar o projeto localmente é necessário apenas:

- Docker
- Docker Compose

Não é necessário instalar manualmente:

- Java
- Maven
- PostgreSQL
- RabbitMQ

Toda a infraestrutura é inicializada automaticamente via containers.

---

# Como executar

Na raiz do projeto:

```bash
docker-compose up --build
```

O comando irá:

- buildar a aplicação Spring Boot
- subir PostgreSQL
- subir RabbitMQ
- executar as migrations Flyway automaticamente
- disponibilizar a API na porta 8080

---

# Como parar os containers

```bash
docker-compose down
```

---

# Rebuild completo

```bash
docker-compose down -v
docker-compose up --build
```

---

# Exemplo docker após execução

![img.png](img.png)

---

# Acessos locais

| Serviço | URL |
|---|---|
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui/index.html |
| RabbitMQ Management | http://localhost:15672 |

RabbitMQ:

```text
Usuário: guest
Senha: guest
```

PostgreSQL:

```text
Host: localhost
Porta: 5432
Database: gestao
Usuário: postgres
Senha: postgres
```

---

# Estrutura do projeto

```text
com.impieri.gestaocontaspagar
├── config
├── domain
│   └── factory
├── dto
├── messaging
├── repository
├── security
├── service
└── web
```

---

# Decisões arquiteturais

O projeto foi organizado buscando separação de responsabilidades:

- `domain`: entidades e regras de negócio
- `factory`: centralização da criação de entidades
- `dto`: objetos de entrada e saída
- `service`: regras de aplicação e transações
- `repository`: persistência com Spring Data JPA
- `messaging`: integração RabbitMQ
- `security`: autenticação JWT
- `web`: controllers REST

A listagem de contas utiliza carregamento do fornecedor associado para evitar problema de N+1 queries.

As regras intrínsecas de negócio ficam concentradas na entidade `Conta`, mantendo invariantes do domínio protegidas independentemente da camada de entrada.

O processamento do CSV foi implementado de forma assíncrona utilizando RabbitMQ, permitindo desacoplamento entre recebimento do arquivo e persistência em banco.

---

# Banco de dados e migrations

O projeto utiliza Flyway para versionamento do banco.

Tabelas criadas:

- `fornecedor`
- `conta`

Configuração utilizada:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

---

# Modelo de domínio

## Fornecedor

Representa o fornecedor associado à conta.

Campos principais:

- `id`
- `nome`

---

## Conta

Representa uma conta a pagar.

Campos principais:

- `id`
- `dataVencimento`
- `dataPagamento`
- `valor`
- `descricao`
- `situacao`
- `fornecedor`

Situações possíveis:

- `PENDENTE`
- `PAGO`
- `CANCELADO`

---

# Regras de negócio implementadas

- conta paga não pode voltar para pendente
- conta paga não pode ser cancelada
- conta cancelada não pode ser paga
- conta paga não pode ser alterada
- conta cancelada não pode ser alterada
- conta paga não pode ser excluída
- valor da conta deve ser positivo
- fornecedor é obrigatório
- conta deve possuir data de vencimento

---

# Autenticação JWT

A API utiliza autenticação JWT para proteger os endpoints.

Os endpoints públicos são:

- `/api/auth/login`
- Swagger/OpenAPI

Todos os demais endpoints exigem autenticação via Bearer Token.

---

## Realizar login

Endpoint:

```http
POST /api/auth/login
```

Exemplo de requisição:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

Resposta esperada:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer"
}
```

---

## Utilizando o token

Após obter o token, enviar no header:

```http
Authorization: Bearer SEU_TOKEN
```

Exemplo:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

# Endpoints principais

Todos os endpoints abaixo exigem autenticação JWT via Bearer Token.

---

## Criar conta

```http
POST /api/contas
```

---

## Listar contas com paginação

```http
GET /api/contas?page=0&size=10
```

Filtros disponíveis:

```http
GET /api/contas?descricao=telefone
GET /api/contas?dataVencimento=2026-06-10
```

---

## Buscar conta por ID

```http
GET /api/contas/{id}
```

---

## Atualizar conta

```http
PUT /api/contas/{id}
```

---

## Alterar situação da conta

```http
PATCH /api/contas/{id}/situacao?situacao=PAGO
```

Situações possíveis:

- `PENDENTE`
- `PAGO`
- `CANCELADO`

---

## Excluir conta

```http
DELETE /api/contas/{id}
```

---

## Relatório de total pago por período

```http
GET /api/contas/relatorio/total-pago?dataInicio=2026-01-01&dataFim=2026-12-31
```

---

## Importação assíncrona via CSV

```http
POST /api/contas/import
```

Content-Type:

```text
multipart/form-data
```

A API recebe o arquivo CSV, publica uma mensagem no RabbitMQ e o processamento ocorre de forma assíncrona através de um consumer.

---

# Formato esperado do CSV

```csv
fornecedor_nome,data_vencimento,valor,descricao
Claro,2026-06-10,150.75,Conta de telefone
TIM,2026-06-15,89.90,Conta de internet
Vivo,2026-06-20,120.00,Conta corporativa
```

---

# Fluxo assíncrono da importação

```text
POST /api/contas/import
        ↓
Controller recebe CSV
        ↓
Producer publica mensagem no RabbitMQ
        ↓
API responde 202 Accepted
        ↓
Consumer processa o arquivo em background
        ↓
Dados são persistidos no banco
```

---

# Tratamento de exceções

A API possui tratamento global de exceções utilizando `@RestControllerAdvice`.

Exemplos tratados:

- entidade não encontrada
- erros de validação
- parâmetros obrigatórios ausentes
- corpo inválido da requisição
- regras de negócio inválidas

Exemplo de resposta:

```json
{
  "timestamp": "2026-05-23T22:10:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Conta não encontrada",
  "path": "/api/contas/uuid"
}
```

---

# Swagger/OpenAPI

A documentação da API fica disponível em:

```text
http://localhost:8080/swagger-ui/index.html
```

O Swagger possui suporte a autenticação JWT via botão `Authorize`.

Fluxo:

1. realizar login
2. copiar token retornado
3. clicar em `Authorize`
4. informar:

```text
Bearer SEU_TOKEN
```

---

# Testando com Postman

O projeto possui uma collection Postman pronta para importação.

Arquivo:

```text
postman/TOTVS_Desafio.postman_collection.json
```

A collection contém exemplos para todos os endpoints da API.

---

## Como importar

1. Abra o Postman
2. Clique em `Import`
3. Selecione o arquivo:

```text
TOTVS_Desafio.postman_collection.json
```

4. Execute os requests da collection

---

# Fluxo sugerido para testar a aplicação

## 1. Realizar login JWT

Objetivos validados:

- autenticação
- geração de token JWT

Endpoint:

```http
POST /api/auth/login
```

---

## 2. Configurar Bearer Token

Após realizar login:

- copiar token retornado
- configurar Bearer Token no Postman ou Swagger

Header utilizado:

```http
Authorization: Bearer TOKEN
```

---

## 3. Importar contas via CSV

Objetivos validados:

- upload multipart/form-data
- mensageria RabbitMQ
- processamento assíncrono
- persistência em lote

Endpoint:

```http
POST /api/contas/import
```

---

## 4. Criar uma conta manualmente

Objetivos validados:

- Bean Validation
- validação de fornecedor
- persistência individual

Endpoint:

```http
POST /api/contas
```

---

## 5. Listar contas com paginação

Objetivos validados:

- paginação
- filtros
- carregamento eficiente do fornecedor

Endpoint:

```http
GET /api/contas?page=0&size=10
```

---

## 6. Testar filtros

### Por descrição

```http
GET /api/contas?descricao=telefone
```

### Por data de vencimento

```http
GET /api/contas?dataVencimento=2026-06-10
```

### Combinado com paginação

```http
GET /api/contas?page=0&size=5&descricao=internet
```

---

## 7. Buscar conta por ID

Objetivo validado:

- recuperação individual da entidade

Endpoint:

```http
GET /api/contas/{id}
```

---

## 8. Atualizar uma conta

Objetivos validados:

- atualização de dados
- regras de domínio
- alteração de fornecedor

Endpoint:

```http
PUT /api/contas/{id}
```

---

## 9. Alterar situação da conta

Objetivos validados:

- transição de estados
- invariantes do domínio

Endpoints:

```http
PATCH /api/contas/{id}/situacao?situacao=PAGO
PATCH /api/contas/{id}/situacao?situacao=CANCELADO
```

---

## 10. Validar regra de negócio inválida

Exemplo:

tentar voltar uma conta paga para `PENDENTE`.

Resultado esperado:

```http
400 Bad Request
```

Endpoint:

```http
PATCH /api/contas/{id}/situacao?situacao=PENDENTE
```

---

## 11. Consultar relatório de total pago

Objetivos validados:

- agregação
- filtro por período

Endpoint:

```http
GET /api/contas/relatorio/total-pago
```

---

## 12. Excluir conta

Objetivos validados:

- exclusão
- proteção de contas pagas

Endpoint:

```http
DELETE /api/contas/{id}
```

---

# Testes automatizados

O projeto possui testes unitários cobrindo:

- regras de domínio
- services
- controllers
- mensageria
- factories
- autenticação JWT

Execução:

Linux/Mac:

```bash
./mvnw test
```

Windows:

```powershell
.\mvnw.cmd test
```

---

# Build local

Linux/Mac:

```bash
./mvnw clean package
```

Windows:

```powershell
.\mvnw.cmd clean package
```