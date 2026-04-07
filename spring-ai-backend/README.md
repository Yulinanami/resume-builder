# spring-ai-backend

Spring Boot backend for AI chat and RAG with Spring AI.

## Stack

- Java 21
- Spring Boot 3 (MVC)
- Spring AI (OpenAI model + pgvector vector store)
- MySQL (business relational data)
- PostgreSQL + pgvector (vector retrieval)

## Project Structure

```text
spring-ai-backend/
  src/main/java/com/resumebuilder/springaibackend/
    config/
    controller/
    dto/
    service/
  src/main/resources/application.yml
  docker-compose.yml
  .env.example
```

## 1) Start databases (MySQL + pgvector)

```bash
docker compose up -d
```

Run this command under `spring-ai-backend/` directory.

## 2) Configure env

Copy `.env.example` to your local env mechanism.

Required minimum env:

```bash
OPENAI_API_KEY=your_api_key_here
```

Defaults already set:

- MySQL database: `resume-builder`
- Default chat model: `gpt-5.4`

If you use OpenAI-compatible providers, set:

```bash
OPENAI_BASE_URL=https://your-provider-base-url
OPENAI_CHAT_MODEL=your-chat-model
OPENAI_EMBEDDING_MODEL=your-embedding-model
```

## 3) Run backend

```bash
mvn spring-boot:run
```

Default base URL: `http://localhost:8080`

## APIs

### POST `/api/ai/chat`

Request:

```json
{
  "message": "鐢喗鍨滄导妯哄鏉╂瑦顔岀粻鈧崢鍡樺伎鏉?
}
```

Response:

```json
{
  "answer": "..."
}
```

### POST `/api/ai/chat/stream`

Request body is same as `/chat`.

Response type: `text/event-stream`

### POST `/api/ai/rag/documents`

Request:

```json
{
  "documents": [
    {
      "sourceId": "resume-guideline-1",
      "content": "STAR 濞夋洖鍨楦跨殶閸︾儤娅欓妴浣锋崲閸斅扳偓浣筋攽閸斻劊鈧胶绮ㄩ弸?,
      "metadata": {
        "category": "guideline"
      }
    }
  ]
}
```

Response:

```json
{
  "inserted": 1
}
```

### POST `/api/ai/rag/query`

Request:

```json
{
  "query": "妞ゅ湱娲扮紒蹇撳坊閹簼绠為崘娆愭纯閺堝鍣洪崠鏍波閺?,
  "topK": 5
}
```

Response:

```json
{
  "answer": "...",
  "sources": [
    {
      "sourceId": "resume-guideline-1",
      "content": "...",
      "metadata": {
        "category": "guideline"
      }
    }
  ]
}
```

## Frontend Integration Suggestion

- Frontend only calls backend APIs.
- Keep provider key and model routing only in backend.
- For your current Vue app, replace direct `fetch /v1/chat/completions` with calls to `http://localhost:8080/api/ai/*`.

### Separate Chat / Embedding Providers

You can configure Chat and Embedding to use different OpenAI-compatible endpoints:

```bash
OPENAI_CHAT_BASE_URL=http://localhost:8317
OPENAI_CHAT_API_KEY=xxx
OPENAI_CHAT_COMPLETIONS_PATH=/v1/chat/completions

OPENAI_EMBEDDING_BASE_URL=https://api.openai.com
OPENAI_EMBEDDING_API_KEY=xxx
OPENAI_EMBEDDINGS_PATH=/v1/embeddings
OPENAI_EMBEDDING_MODEL=text-embedding-3-small
```

Important: if you keep the default path settings (`/v1/chat/completions`, `/v1/embeddings`), do not append `/v1` in `*_BASE_URL`.
### Local Env Loading

`application.yml` imports `spring-ai-backend/.env` via:

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
```

So you can keep secrets in `.env` and avoid hardcoding keys in YAML.