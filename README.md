# DiceRPG Kotlin

Projeto focado no app Android em Kotlin.

## Estrutura atual

- `android-app/` → código ativo do aplicativo Kotlin/Android.
- `docs/` → documentação funcional ainda útil para regras de dados.
- `arquivos_obsoletos/` → materiais legados removidos do fluxo principal (backend Python, protótipos web e libs JS antigas).

## Backend Python ainda é necessário?

Para o **app Kotlin atual deste repositório**, o backend Python **não é obrigatório** para compilar e evoluir o motor de rolagem/local UI Android.  
Ele foi movido para `arquivos_obsoletos/backend/` como legado, caso você queira reaproveitar APIs no futuro.

## Executar app Android

```bash
cd android-app
./gradlew assembleDebug
```
