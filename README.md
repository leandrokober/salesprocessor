# Sales Processor API

API Spring Boot para processar vendas com persistência em PostgreSQL, mensageria com RabbitMQ, idempotência por ID da venda e geração assíncrona de comprovante PDF.

O projeto foi estruturado para demonstrar um fluxo ponta a ponta simples, testável e fácil de avaliar.

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
4. Se a venda já existe, a API retorna `200 OK` com os dados existentes.
5. Se a venda é nova, o sistema calcula imposto e valor líquido.
6. A venda e salva no PostgreSQL com status `PENDENTE`.
7. Apos o commit no banco, o publisher envia o ID da venda para o RabbitMQ.
8. A API responde imediatamente `201 Created`.
9. O consumer recebe a mensagem da fila de forma assíncrona.
10. O consumer verifica novamente se a venda ja foi processada.
11. O sistema gera um comprovante PDF em `target/comprovantes/`.
12. A venda é atualizada para `PROCESSADA`.

## Endpoints

### Swagger

```text
http://localhost:8080/swagger-ui/index.html
```

O Swagger está liberado sem login/senha para facilitar a avaliacao.

### Criar venda

```http
POST /api/vendas
```

Header obrigatorio:

```text
X-API-KEY: sales-token-secreto-123
```

Importante: a API key é validada exatamente como definida. Espaços extras ou valores diferentes retornam `401 Unauthorized`.

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

Erros são retornados em formato padronizado:

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

Serviços expostos:

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

### 2. Rodar a aplicação

```powershell
.\mvnw.cmd spring-boot:run
```

A API subirá em:

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

## Como Testar a Idempotência

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

Observação: a idempotência usa o ID exato da venda. Por exemplo, `VENDA-002` e `VENDA-0002` sao IDs diferentes.

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

Resultado esperado após processamento:

```text
venda.processada.queue  0  0  1
```

Isso indica que existe um consumer ativo e que não há mensagem parada na fila.

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

Os logs de negócio aparecem na aplicação Spring Boot, não no log do RabbitMQ.

Exemplo esperado:

```text
Sale processing event published. saleId=VENDA-001
Sale processing message received. saleId=VENDA-001, queue=venda.processada.queue
Generating sale receipt PDF. saleId=VENDA-001, customerName=Maria Silva
Sale receipt PDF generated. saleId=VENDA-001, filePath=...\target\comprovantes\comprovante-VENDA-001.pdf
Sale processing completed. saleId=VENDA-001, status=PROCESSADA
```

O RabbitMQ normalmente mostra conexões, autenticação e estado do broker, por exemplo:

```text
accepting AMQP connection
user 'guest' authenticated and granted access to vhost '/'
```

## Rodar Testes

```powershell
.\mvnw.cmd test
```

Os testes cobrem:

- Validação da API key
- Tratamento global de erros
- Retorno `201 Created` para venda nova
- Retorno `200 OK` para venda já existente
- Idempotêmcia no service
- Publicação após commit da transação
- Consumer ignorando mensagem duplicada
- Geração de comprovante PDF

## Troubleshooting

### Swagger continua mostrando comportamento antigo

Reinicie a aplicação Spring Boot. Alterações em Java só entram apos recompilar/reiniciar.

```powershell
Ctrl + C
.\mvnw.cmd spring-boot:run
```

### Porta 8080 ocupada

Verificar processo:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
```

### RabbitMQ não conecta

Verifique se o container está rodando:

```powershell
docker ps
```

Verifique logs:

```powershell
docker logs --tail 100 sales-rabbitmq
```

### Banco recusou conexão

Suba o Docker Compose:

```powershell
docker compose up -d
```

## Observações Técnicas

- A API key é propositalmente mockada para simplificar a avaliação.
- O Swagger foi liberado sem login para facilitar testes por recrutadores.
- A publicação no RabbitMQ ocorre apenas após o commit da transação no banco.
- O retorno inicial da venda nova pode mostrar `PENDENTE`, pois o processamento do PDF é assíncrono.
- A confirmação final do fluxo e o status `PROCESSADA` no banco e o PDF criado em `target/comprovantes/`.
