# Sales Processor API

API Spring Boot para processar vendas com persistencia em PostgreSQL, mensageria com RabbitMQ, idempotencia por ID da venda e geracao assincrona de comprovante PDF.

O projeto foi estruturado para demonstrar um fluxo ponta a ponta simples, testavel e facil de avaliar em entrevista tecnica.

## Stack

- Java 17
- Spring Boot 3.5
- Spring Web
- Spring Data JPA
- Spring Security
- Spring AMQP
- PostgreSQL
- RabbitMQ
- Swagger/OpenAPI
- Tratamento global de erros com `@ControllerAdvice`
- JUnit 5 + Mockito
- Docker Compose

## Fluxo Ponta a Ponta

1. O cliente envia um `POST /api/vendas` com o JSON da venda e o header `X-API-KEY`.
2. O controller valida a API key mockada.
3. O service aplica idempotencia pelo campo `id`.
4. Se a venda ja existe, a API retorna `200 OK` com os dados existentes.
5. Se a venda e nova, o sistema calcula imposto e valor liquido.
6. A venda e salva no PostgreSQL com status `PENDENTE`.
7. Apos o commit no banco, o publisher envia o ID da venda para o RabbitMQ.
8. A API responde imediatamente `201 Created`.
9. O consumer recebe a mensagem da fila de forma assincrona.
10. O consumer verifica novamente se a venda ja foi processada.
11. O sistema gera um comprovante PDF em `target/comprovantes/`.
12. A venda e atualizada para `PROCESSADA`.

## Endpoints

### Swagger

```text
http://localhost:8080/swagger-ui/index.html
```

O Swagger esta liberado sem login/senha para facilitar a avaliacao.

### Criar venda

```http
POST /api/vendas
```

Header obrigatorio:

```text
X-API-KEY: sales-token-secreto-123
```

Importante: a API key e validada exatamente como definida. Espacos extras ou valores diferentes retornam `401 Unauthorized`.

Body de exemplo:

```json
{
  "id": "VENDA-001",
  "clienteNome": "Maria Silva",
  "valorBruto": 1000
}
```

Respostas esperadas:

```text
201 Created      Venda nova criada e enviada para processamento
200 OK           Venda ja existente retornada por idempotencia
400 Bad Request  JSON invalido ou campos obrigatorios invalidos
401 Unauthorized API key ausente ou invalida
```

Erros sao retornados em formato padronizado:

```json
{
  "timestamp": "2026-05-17T17:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Acesso negado: X-API-KEY invalida ou ausente",
  "path": "/api/vendas"
}
```

## Como Executar

### 1. Subir PostgreSQL e RabbitMQ

Na raiz do projeto:

```powershell
docker compose up -d
```

Servicos expostos:

```text
PostgreSQL: localhost:5432
RabbitMQ AMQP: localhost:5672
RabbitMQ Management: http://localhost:15672
```

Credenciais do RabbitMQ:

```text
usuario: guest
senha: guest
```

Credenciais do PostgreSQL:

```text
database: db_sales_processor
usuario: datainfo_user
senha: password123
```

### 2. Rodar a aplicacao

```powershell
.\mvnw.cmd spring-boot:run
```

A API subira em:

```text
http://localhost:8080
```

### 3. Abrir o Swagger

```text
http://localhost:8080/swagger-ui/index.html
```

Preencha o header:

```text
X-API-KEY = sales-token-secreto-123
```

Use um JSON como:

```json
{
  "id": "VENDA-001",
  "clienteNome": "Maria Silva",
  "valorBruto": 1000
}
```

## Como Testar a Idempotencia

1. Envie o `POST /api/vendas` com um ID novo, por exemplo:

```json
{
  "id": "VENDA-TESTE-001",
  "clienteNome": "Maria Silva",
  "valorBruto": 1000
}
```

Resultado esperado:

```text
201 Created
```

2. Envie exatamente o mesmo JSON novamente.

Resultado esperado:

```text
200 OK
```

Observacao: a idempotencia usa o ID exato da venda. Por exemplo, `VENDA-002` e `VENDA-0002` sao IDs diferentes.

## Como Validar no Banco

Entrar no PostgreSQL:

```powershell
docker exec -it sales-postgres psql -U datainfo_user -d db_sales_processor
```

Consultar vendas:

```sql
select id_venda, cliente_nome, valor_bruto, valor_imposto, valor_liquido, status_processamento, data_cadastro
from tb_vendas
order by data_cadastro desc;
```

Fluxo esperado:

```text
Logo apos o POST: PENDENTE
Apos o consumer processar: PROCESSADA
```

## Como Validar no RabbitMQ

Painel web:

```text
http://localhost:15672
```

Credenciais:

```text
guest / guest
```

Fila usada pelo projeto:

```text
venda.processada.queue
```

Exchange:

```text
venda.exchange
```

Routing key:

```text
venda.routingKey
```

Verificar fila via terminal:

```powershell
docker exec sales-rabbitmq rabbitmqctl list_queues name messages_ready messages_unacknowledged consumers
```

Resultado esperado apos processamento:

```text
venda.processada.queue  0  0  1
```

Isso indica que existe um consumer ativo e que nao ha mensagem parada na fila.

## Como Validar o PDF

O comprovante e gerado pelo consumer em:

```text
target/comprovantes/
```

Nome do arquivo:

```text
comprovante-<ID_DA_VENDA>.pdf
```

Exemplo:

```text
target/comprovantes/comprovante-VENDA-001.pdf
```

## Logs Importantes

Os logs de negocio aparecem na aplicacao Spring Boot, nao no log do RabbitMQ.

Exemplo esperado:

```text
Sale processing event published. saleId=VENDA-001
Sale processing message received. saleId=VENDA-001, queue=venda.processada.queue
Generating sale receipt PDF. saleId=VENDA-001, customerName=Maria Silva
Sale receipt PDF generated. saleId=VENDA-001, filePath=...\target\comprovantes\comprovante-VENDA-001.pdf
Sale processing completed. saleId=VENDA-001, status=PROCESSADA
```

O RabbitMQ normalmente mostra conexoes, autenticacao e estado do broker, por exemplo:

```text
accepting AMQP connection
user 'guest' authenticated and granted access to vhost '/'
```

## Rodar Testes

```powershell
.\mvnw.cmd test
```

Os testes cobrem:

- Validacao da API key
- Tratamento global de erros
- Retorno `201 Created` para venda nova
- Retorno `200 OK` para venda ja existente
- Idempotencia no service
- Publicacao apos commit da transacao
- Consumer ignorando mensagem duplicada
- Geracao de comprovante PDF

## Troubleshooting

### Swagger continua mostrando comportamento antigo

Reinicie a aplicacao Spring Boot. Alteracoes em Java so entram apos recompilar/reiniciar.

```powershell
Ctrl + C
.\mvnw.cmd spring-boot:run
```

### Porta 8080 ocupada

Verificar processo:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
```

### RabbitMQ nao conecta

Verifique se o container esta rodando:

```powershell
docker ps
```

Verifique logs:

```powershell
docker logs --tail 100 sales-rabbitmq
```

### Banco recusou conexao

Suba o Docker Compose:

```powershell
docker compose up -d
```

## Observacoes Tecnicas

- A API key e propositalmente mockada para simplificar a avaliacao.
- O Swagger foi liberado sem login para facilitar testes por recrutadores.
- A publicacao no RabbitMQ ocorre apenas apos o commit da transacao no banco.
- O retorno inicial da venda nova pode mostrar `PENDENTE`, pois o processamento do PDF e assincrono.
- A confirmacao final do fluxo e o status `PROCESSADA` no banco e o PDF criado em `target/comprovantes/`.
