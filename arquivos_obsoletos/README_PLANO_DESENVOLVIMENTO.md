# DiceRpg — Plano de Desenvolvimento do Produto

## Objetivo
Entregar um aplicativo **chat-first para RPG assíncrono (PbP)** inspirado nos pontos fortes do mRPG, com foco em:
- campanhas por texto;
- múltiplas personas (jogador, personagem, narrador e NPC);
- fichas customizáveis e macros;
- recrutamento de mesas (LFP);
- operação em Android, iOS e Web.

> Este documento **não substitui** o `README.md` original. Ele organiza um plano de execução para transformar o levantamento analítico do projeto em produto entregável.

---

## Escopo funcional (MVP → V1)

### 1) Núcleo de campanha e chat (MVP)
- Criação e gerenciamento de campanhas.
- Chat assíncrono persistente por campanha.
- Participação em múltiplas campanhas simultaneamente.
- Mensagens com texto e imagem.
- Notificações básicas.

### 2) Personas e interpretação (MVP)
- Envio de mensagem como:
  - Usuário;
  - Personagem;
  - Narrador;
  - NPC.
- Alternância rápida da persona ativa no composer.

### 3) Fichas customizáveis (MVP+)
- Template de ficha por campanha.
- Campos tipados (texto, número, boolean, lista simples).
- Abas mínimas: **Sheet**, **Inventory** e **Log**.
- Associação de personagem ao jogador.

### 4) Dados e macros (MVP+)
- Rolagem padrão (`d20`, `2d6+3`, etc.).
- Macros salvas por personagem/campanha.
- Inserção de resultado no chat.

### 5) Descoberta/LFP (V1)
- Listagem de campanhas públicas.
- Busca textual.
- Filtros por tipo de campanha.
- Fluxo de pedido de entrada e aprovação do GM.

### 6) Preservação e migração (V1)
- Exportação de mensagens de campanha.
- Backup básico dos templates de ficha.

---

## Fora do escopo inicial
- Battlemap tático com grid/tokens.
- Tracker de combate nativo completo.
- Marketplace de módulos/plugins.
- Áudio/voz embutidos.

Esses itens podem entrar em um roadmap de longo prazo, após validação do núcleo social + ficha + chat.

---

## Requisitos não funcionais
- **Confiabilidade do chat**: fila de envio, retry, deduplicação.
- **Escalabilidade**: arquitetura orientada a eventos para mensagens.
- **Segurança**: autenticação robusta, rate limit, proteção anti-spam.
- **Moderação**: report, análise de conteúdo reportado, ações administrativas.
- **Observabilidade**: logs estruturados, métricas, tracing.
- **Privacidade**: políticas claras de retenção/exclusão de dados.

---

## Arquitetura proposta (alto nível)

### Frontend
- **Mobile**: Flutter ou React Native.
- **Web**: React + TypeScript.

### Backend
- API HTTP + WebSocket para chat em tempo real.
- Serviço de autenticação (email/social/phone opcional por fase).
- Serviço de campanhas/personagens/fichas.
- Serviço de rolagem e macros (sandbox de execução de fórmula).
- Serviço de mídia (upload e CDN).

### Dados
- PostgreSQL (dados relacionais principais).
- Redis (presença, cache, fila curta).
- Object Storage (imagens/exportações).

### Infra
- Containerização (Docker), CI/CD, ambientes: dev/stage/prod.
- Deploy em nuvem com autoscaling básico.

---

## Modelo de domínio (resumo)
- **User**
- **Campaign**
- **CampaignMembership** (papéis: GM, Player, Moderator)
- **Character**
- **SheetTemplate**
- **SheetField / SheetValue**
- **Message**
- **PersonaState**
- **DiceRoll / Macro**
- **JoinRequest (LFP)**
- **Report / ModerationAction**

---

## Roadmap por fases

### Fase 0 — Fundação (2 semanas)
- Definição de produto (PRD enxuto).
- Definição técnica (ADR inicial).
- Setup de repositórios, CI, ambientes e observabilidade básica.

**Critério de saída:** pipeline funcional + arquitetura aprovada.

### Fase 1 — MVP de Chat & Campanha (4–6 semanas)
- Autenticação básica.
- CRUD de campanha.
- Chat assíncrono persistente com WebSocket.
- Mensagem de texto/imagem.
- Lista de campanhas do usuário.

**Critério de saída:** grupos conseguem jogar por texto continuamente.

### Fase 2 — Personas + Personagens + Ficha base (4 semanas)
- Personas por mensagem.
- CRUD de personagens.
- Associação personagem↔jogador.
- Template simples de ficha e abas Sheet/Inventory/Log.

**Critério de saída:** experiência de interpretação multi-persona estável.

### Fase 3 — Dados e Macros (3–4 semanas)
- Engine de rolagem de dados.
- Macros persistidas por personagem/campanha.
- Renderização clara de resultados no chat.

**Critério de saída:** ações repetitivas do jogo podem ser automatizadas.

### Fase 4 — LFP e onboarding social (3 semanas)
- Catálogo de campanhas públicas.
- Busca e filtros.
- Solicitação/aceite de entrada.

**Critério de saída:** novos jogadores encontram e entram em mesas.

### Fase 5 — Exportação, moderação e hardening (3 semanas)
- Exportação de mensagens.
- Fluxo de report/moderação.
- Correções de estabilidade e performance.

**Critério de saída:** base pronta para beta aberto.

---

## Plano de qualidade
- Testes unitários (domínio e regras de rolagem).
- Testes de integração (API + banco + filas).
- Testes E2E (fluxos principais: criar campanha, conversar, trocar persona, rolar dados, entrar via LFP).
- Testes de carga no chat (picos simulados).
- SLOs iniciais:
  - envio de mensagem P95 < 500ms (sem mídia);
  - disponibilidade mensal >= 99.5% no beta.

---

## Métricas de produto
- D1/D7 retenção de jogadores.
- Mensagens por campanha/semana.
- Campanhas ativas por mês.
- Taxa de sucesso em pedidos de entrada (LFP).
- Uso de macros por sessão.
- Tempo médio para primeira sessão após cadastro.

---

## Riscos e mitigação
- **Instabilidade de chat** → arquitetura com fila, retry e monitoramento agressivo.
- **Complexidade excessiva da ficha** → começar com schema simples e expandir iterativamente.
- **Abuso em campanhas públicas** → moderação e rate limit desde o V1.
- **Escopo inflado por features de VTT tático** → manter foco no núcleo chat-first.

---

## Equipe mínima sugerida
- 1 Product Manager / Product Owner.
- 1 Tech Lead.
- 2–3 Engenheiros Full Stack.
- 1 Engenheiro Mobile (ou perfil multiplataforma forte).
- 1 Designer de Produto.
- 1 QA (com automação).

---

## Entregáveis
1. Aplicativo funcional (Web + Mobile) com MVP completo.
2. Documentação técnica (arquitetura, APIs, modelo de dados).
3. Guia operacional (incidentes, monitoramento, backup/exportação).
4. Plano de evolução pós-beta.

---

## Próximos passos imediatos (execução)
1. Validar este plano com stakeholders.
2. Transformar fases em backlog (épicos e histórias).
3. Priorizar MVP estrito e congelar escopo da Fase 1.
4. Iniciar Fase 0 com metas semanais e demos quinzenais.

