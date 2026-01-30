# 📱 Guia Passo a Passo - Compilar Diablo Access para Android

## 📋 Situação Atual

✅ **ARQUIVOS CRIADOS:**
- `build-android.bat` - Script de compilação
- `ANDROID_BUILD_AND_PLAY.md` - Guia completo de instalação e jogo
- `BUILD_STATUS.md` - Status atual da instalação

⏳ **EM ANDAMENTO:**
- Baixando Android Studio via winget (~1GB)
- Instalando pacotes necessários
- Configurando Android SDK e JDK 17

---

## 🎯 Resumo do Que Acontece:

1. **Winget** está baixando Android Studio automaticamente
2. Android Studio inclui:
   - **JDK 17** (necessário para compilar)
   - **Android SDK**
   - **Gradle 8.13**
3. Tudo instalado em: `C:\Program Files\Android\Android Studio\`

**Tempo estimado:** 15-30 minutos (primeira vez)

---

## ⏸️ ENQUANTO AGUARDA (20-30 min):

### Opção 1: Baixar Arquivos MPQ Necessários

Enquanto o Android Studio baixa, prepare os arquivos do jogo:

#### **OBRIGATÓRIO (um deles):**

**Opção A - Diablo Completo:**
- Se você tem o CD do Diablo: copie `DIABDAT.MPQ`
- Se comprou na GoG: extraia `DIABDAT.MPQ` da instalação

**Opção B - Shareware GRÁTIS:**
```bash
# Baixe spawn.mpq (~300MB)
https://github.com/diasurgical/devilutionx-assets/releases/latest/download/spawn.mpq
```

#### **RECOMENDADO (recursos melhorados):**
```bash
# Baixe devilutionx.mpq (~600KB)
https://github.com/diasurgical/devilutionx-assets/releases/latest/download/devilutionx.mpq
```

### Opção 2: Familiarize-se com os Controles

Leia o arquivo `ANDROID_BUILD_AND_PLAY.md` na raiz do projeto - ele explica:
- ✅ Como navegar por gestos
- ✅ Como usar controles virtuais
- ✅ Como configurar arquivos MPQ
- ✅ Como jogar

---

## 🚀 QUANDO INSTALAÇÃO TERMINAR:

### Passo 1: Verificar

```cmd
REM Verificar se instalou
winget list | findstr Android Studio

REM Deve mostrar algo como:
REM Google.AndroidStudio   2024.X.X.X   [instalado]
```

### Passo 2: Compilar

```cmd
REM Execute o script que criei:
build-android.bat

REM Ou manualmente:
cd android-project
gradlew.bat assembleDebug
```

### Passo 3: Localizar APK

O APK estará em:
```
android-project/app/build/outputs/apk/debug/app-debug.apk
```

### Passo 4: Instalar no Android

```cmd
REM Conectar dispositivo Android
adb devices

REM Instalar
adb install android-project/app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 Depois de Instalar no Android:

### 1. Primeira Abertura

1. **ABRA O APP UMA VEZ** (importante!)
2. Ele vai pedir arquivos MPQ
3. Feche o app (ele criou as pastas necessárias)

### 2. Copiar Arquivos MPQ

**Local da pasta no Android:**
```
Armazenamento Interno/Android/data/org.diasurgical.devilutionx/files/
```

**Como copiar:**

**Método A - Windows Explorer:**
1. Conecte dispositivo via USB
2. Abrir "Este PC" → Seu Dispositivo → Armazenamento Interno
3. Navegue até: `Android → data → org.diasurgical.devilutionx → files`
4. Copie todos os arquivos `.mpq` para lá

**Método B - ADB:**
```cmd
adb push DIABDAT.MPQ /sdcard/Android/data/org.diasurgical.devilutionx/files/
adb push devilutionx.mpq /sdcard/Android/data/org.diasurgical.devilutionx/files/
```

### 3. Jogar!

1. Abra o app novamente
2. Clique em "Verificar novamente"
3. Clique em "Iniciar"
4. **Aproveite!** 🎮

---

## 🎮 Modo Acessível no Android

Se você habilitou `SCREEN_READER_INTEGRATION`:

### Detecção Automática:

- **TalkBack ATIVO** → Usa leitor de tela do sistema
- **TalkBack INATIVO** → Modo acessível do jogo é ativado

### Gestos Disponíveis:

| Gesto | Ação |
|--------|--------|
| **Deslizar para Direita** | Próxima opção de menu (anuncia em voz) |
| **Deslizar para Esquerda** | Opção anterior de menu (anuncia em voz) |
| **Dois Toques Rápido** | Confirmar/Entrar na seleção |
| **Segurar e Arrastar** | Mover personagem |

### TTS em Português:

O app usará automaticamente **português brasileiro (pt-BR)** se disponível no dispositivo.

---

## 🔧 Se Algo Der Errado:

### Problema: "Não foi possível encontrar DIABDAT.MPQ"

**Solução:**
1. Verifique se copiou para a pasta correta
2. Use um gerenciador de arquivos Android para confirmar
3. Os arquivos devem estar em:
   `/Android/data/org.diasurgical.devilutionx/files/`

### Problema: App trava ou fecha

**Solução:**
1. Certifique-se que copiou TODOS os arquivos necessários
2. Verifique se `devilutionx.mpq` está presente
3. Reinicie o dispositivo

### Problema: Java 8 detectado

**Solução:**
- O Android Studio instalado incluirá JDK 17 automaticamente
- O script `build-android.bat` usa o JDK do Android Studio
- Não precisa instalar Java separadamente

---

## 📞 Próximos Passos Automatizados:

Vou verificar periodicamente se a instalação terminou e compilar para você. Aguarde...
