# DiceRPG Kotlin

Guia prático para configurar e rodar o projeto **DiceRPG** localmente.

## 1) O que existe neste repositório

- `android-app/` → código principal do app Android (Kotlin).
- `docs/` → documentação de regras e funcionamento.
- `arquivos_obsoletos/` → conteúdo legado (não necessário para rodar o app atual).
- `dice-typescript-master/` e `rpg-dice-roller-master/` → referências/protótipos antigos em JavaScript.

> Para executar o produto atual, foque em **`android-app/`**.

---

## 2) Pré-requisitos

Instale os itens abaixo antes de começar:

1. **Android Studio** (versão recente estável).
2. **JDK 17** (normalmente já vem configurado no Android Studio atual).
3. **Android SDK** com:
   - `Android SDK Platform` (API usada pelo projeto)
   - `Android SDK Build-Tools`
   - `Android SDK Platform-Tools` (adb)
4. Um alvo para execução:
   - **Emulador Android** (AVD), ou
   - **Celular Android físico** com modo desenvolvedor + depuração USB.

---

## 3) Rodando via Android Studio (mais fácil)

1. Abra o **Android Studio**.
2. Clique em **Open** e selecione a pasta:
   - `android-app/`
3. Aguarde o Gradle sincronizar o projeto.
4. Selecione um dispositivo:
   - Emulador (AVD) ou celular conectado.
5. Clique em **Run** (▶) para compilar e instalar o app.

Se o Android Studio pedir para instalar componentes do SDK/Gradle, aceite e rode novamente.

---

## 4) Rodando via terminal (CLI)

No Linux/macOS (ou terminal do Android Studio):

```bash
cd android-app
./gradlew assembleDebug
```

Isso gera o APK de debug.

### Opcional: instalar no dispositivo conectado

```bash
cd android-app
./gradlew installDebug
```

---

## 5) Onde fica o APK gerado

Após `assembleDebug`, o APK normalmente fica em:

```text
android-app/app/build/outputs/apk/debug/app-debug.apk
```

Você pode instalar manualmente esse arquivo no dispositivo, se necessário.

---

## 6) Problemas comuns e solução rápida

### Erro de SDK/Build Tools ausente
- Abra o **SDK Manager** no Android Studio.
- Instale os componentes faltantes.
- Rode o build novamente.

### Erro de versão do Java
- Confirme que o projeto está usando **JDK 17**.
- No Android Studio: *File > Settings > Build, Execution, Deployment > Build Tools > Gradle*.

### Emulador não inicia
- Verifique virtualização habilitada (BIOS/UEFI).
- Tente criar outro AVD com imagem x86_64 ou arm64 compatível.

### Dispositivo físico não aparece
- Ative **Depuração USB**.
- Aceite o prompt de autorização RSA no celular.
- Teste com:
  ```bash
  adb devices
  ```

---

## 7) Fluxo recomendado para desenvolvimento

1. Entrar em `android-app/`.
2. Rodar build rápido:
   ```bash
   ./gradlew assembleDebug
   ```
3. Validar no emulador/dispositivo.
4. Só então abrir PR/commit com mudanças.

---

## 8) Observação sobre backend Python

O backend Python legado em `arquivos_obsoletos/backend/` **não é necessário** para rodar o app Android atual.
