# DiceRpg

Implementação inicial das **Fases 0, 1 e 2** do roadmap, com base em `README_PLANO_DESENVOLVIMENTO.md`.

## Entregas por fase

### Fase 0 — Fundação
- Estrutura inicial de backend e frontend criada.
- API FastAPI com versionamento inicial (`v0.1.0`).
- Endpoint de saúde (`GET /health`) para checagem operacional.
- Dependências e setup local definidos em `backend/requirements.txt`.

### Fase 1 — MVP de Chat & Campanha
- Autenticação básica com registro e login JWT:
  - `POST /auth/register`
  - `POST /auth/login`
- CRUD inicial de campanhas (criação e listagem do usuário):
  - `POST /campaigns`
  - `GET /campaigns`
- Chat assíncrono persistente por campanha:
  - `POST /campaigns/{campaign_id}/messages`
  - `GET /campaigns/{campaign_id}/messages`
- Canal WebSocket para tempo real:
  - `WS /ws/campaigns/{campaign_id}`

### Fase 2 — Personas + Personagens + Ficha base
- Personas por mensagem (`persona_type` e `persona_name`).
- CRUD base de personagens:
  - `POST /characters`
  - `GET /campaigns/{campaign_id}/characters`
- Associação personagem↔jogador via `user_id` em `Character`.
- Estrutura de ficha base em JSON com abas:
  - `sheet`
  - `inventory`
  - `log`

## Como rodar localmente

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Acesse:
- API docs: `http://localhost:8000/docs`
- Frontend placeholder: abrir `frontend/index.html`

## Próximos passos sugeridos (Fase 3+)
- Engine robusta de rolagem de dados e macros persistentes.
- Upload de imagem (mensagens com mídia).
- Controle de membership (convite/aprovação) e campanhas públicas (LFP).
- Testes automatizados unitários e de integração.
