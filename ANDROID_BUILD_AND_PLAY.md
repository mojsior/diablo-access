# 📱 Diablo Access - Guia Completo para Android

## 📋 Sumário

Este guia explica passo a passo como:
1. ✅ Compilar Diablo Access para Android
2. ✅ Instalar no dispositivo Android
3. ✅ Configurar os arquivos MPQ necessários
4. ✅ Jogar no Android

---

## 🔨 Parte 1: Compilar para Android

### Pré-requisitos

Você precisará de:

1. **Android Studio** (recomendido) OU **Android SDK + NDK**
2. **JDK 17** (Java Development Kit)
3. **CMake 3.31.0+**
4. **Gradle** (incluso no projeto)
5. **Git** (para clonar o repositório)

### Passo 1: Clonar o Repositório

```bash
git clone https://github.com/seu-usuario/diablo-access.git
cd diablo-access
```

### Passo 2: Preparar o Ambiente

#### Opção A: Usando Android Studio (RECOMENDADO)

1. Abra o Android Studio
2. Selecione **File → Open**
3. Navegue até a pasta `android-project`
4. Selecione **Open**

O Android Studio irá:
- Sincronizar o projeto Gradle
- Baixar o NDK se necessário
- Configurar o CMake automaticamente

#### Opção B: Usando Linha de Comando

```bash
cd android-project

# Verificar se o Gradle está funcionando
./gradlew --version

# Compilar a versão Debug
./gradlew assembleDebug

# OU compilar a versão Release
./gradlew assembleRelease
```

### Passo 3: Habilitar Acessibilidade

Para compilar **COM** o suporte a acessibilidade, você precisa habilitar a flag:

```bash
# No diretório raiz do projeto (android-project)
mkdir -p .cxx
cmake -DANDROID_STL=c++_static -DSCREEN_READER_INTEGRATION=ON -Bbuild
```

Ou edite `app/build.gradle`:

```gradle
externalNativeBuild {
    cmake {
        arguments "-DANDROID_STL=c++_static",
                   "-DSCREEN_READER_INTEGRATION=ON"  // ← ADICIONE ISSO
        abiFilters 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'
    }
}
```

### Passo 4: Build no Android Studio

1. No Android Studio: **Build → Make Project** (ou Ctrl+F9)
2. Aguarde o build terminar (pode levar alguns minutos na primeira vez)

### Passo 5: Localizar o APK

O APK compilado estará em:

```
android-project/app/build/outputs/apk/debug/app-debug.apk
```

Para Release:

```
android-project/app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## 📦 Parte 2: Arquivos MPQ Necessários

### O Que São Arquivos MPQ?

**MPQ** = MoPaQ (arquivo de dados do Blizzard) - É como um ZIP que contém todos os dados do jogo (gráficos, sons, textos, etc.).

**ATENÇÃO:** Não é `.qt` mas sim `.mpq`! 🎮

### Arquivos Necessários

#### Obrigatório:
```
✅ DIABDAT.MPQ         - Dados principais do Diablo (versão completa)
OU
✅ spawn.mpq           - Versão shareware (gratuita)
```

#### Opcional (Hellfire):
```
⭐ hellfire.mpq        - Expansão Hellfire
⭐ hfmonk.mpq          - Dados de monks do Hellfire
⭐ hfmusic.mpq         - Música do Hellfire
⭐ hfvoice.mpq         - Voz do Hellfire
```

#### Recursos Adicionais:
```
🌐 devilutionx.mpq     - Gráficos e recursos melhorados (RECOMENDADO)
🌐 fonts.mpq           - Fontes para chinês/coreano/japonês
🌐 pl.mpq              - Voz em polonês
🌐 ru.mpq              - Voz em russo
```

### Onde Obter os Arquivos MPQ?

#### Opção 1: CD Original do Diablo
- Localize `DIABDAT.MPQ` no seu CD do Diablo

#### Opção 2: GoG (Good Old Games)
1. Compre Diablo em: https://www.gog.com/game/diablo
2. Instale no seu PC
3. Copie `DIABDAT.MPQ` da pasta de instalação

#### Opção 3: Versão Shareware (GRÁTIS)
1. Baixe o shareware: https://github.com/diasurgical/devilutionx-assets/releases/latest/download/spawn.mpq
2. Use no lugar de `DIABDAT.MPQ`

#### Opção 4: devilutionx.mpq (RECOMENDADO)
1. Baixe de: https://github.com/diasurgical/devilutionx-assets/releases/latest/download/devilutionx.mpq
2. Contém gráficos melhorados e recursos adicionais

---

## 📲 Parte 3: Instalar no Android

### Método 1: ADB (USB Debugging)

#### Passo 1: Habilitar USB Debugging no Dispositivo

1. Vá em **Configurações → Sobre o Telefone**
2. Toque 7 vezes em **Número da Versão** (ou **Build Number**)
3. Volte para **Configurações**
4. Entre em **Opções do Desenvolvedor**
5. Habilite **Depuração USB**

#### Passo 2: Instalar o APK

```bash
# Instalar o APK
adb install android-project/app/build/outputs/apk/debug/app-debug.apk

# Verificar se instalou
adb shell pm list packages | grep devilutionx
```

### Método 2: Copiar e Instalar Manualmente

1. Copie o APK para o dispositivo (via USB, Google Drive, etc.)
2. Abra o **Gerenciador de Arquivos** no Android
3. Navegue até a pasta onde copiou o APK
4. Toque no arquivo `app-debug.apk`
5. Confirme a instalação

### Método 3: Google Play (Versão Oficial)

O DevilutionX está disponível no Google Play:
- **Package:** `org.diasurgical.devilutionx`
- Link: https://play.google.com/store/apps/details?id=org.diasurgical.devilutionx

**NOTA:** A versão do Google Play pode não ter as modificações de acessibilidade do Diablo Access!

---

## 🗂️ Parte 4: Configurar Arquivos MPQ no Android

### Passo 1: Abrir o App Pela Primeira Vez

1. **IMPORTANTE:** Abra o app DevilutionX/Diablo Access uma vez
2. O app mostrará uma mensagem: **"Não foi possível encontrar o arquivo de dados (.MPQ)"**
3. Isso é **normal** - o app criou as pastas necessárias
4. Feche o app

### Passo 2: Conectar via USB

1. Conecte o dispositivo ao PC via cabo USB
2. No dispositivo, permita o acesso aos dados (veja as imagens abaixo)

### Passo 3: Copiar os Arquivos MPQ

#### Localizar a Pasta Correta:

```
Armazenamento Interno/Android/data/org.diasurgical.devilutionx/files/
```

#### Métodos para Copiar:

**Opção A: Usando Windows Explorer**
1. Abra o Explorer no Windows
2. Navegue até o dispositivo Android
3. Vá para: `Internal Storage → Android → data → org.diasurgical.devilutionx → files`
4. Copie todos os arquivos `.mpq` para esta pasta

**Opção B: Usando ADB**
```bash
# Copiar DIABDAT.MPQ
adb push DIABDAT.MPQ /sdcard/Android/data/org.diasurgical.devilutionx/files/

# Copiar devilutionx.mpq (recomendado)
adb push devilutionx.mpq /sdcard/Android/data/org.diasurgical.devilutionx/files/

# Copiar arquivos Hellfire (se tiver)
adb push hellfire.mpq /sdcard/Android/data/org.diasurgical.devilutionx/files/
adb push hfmonk.mpq /sdcard/Android/data/org.diasurgical.devilutionx/files/
adb push hfmusic.mpq /sdcard/Android/data/org.diasurgical.devilutionx/files/
adb push hfvoice.mpq /sdcard/Android/data/org.diasurgical.devilutionx/files/
```

**Opção C: Usando Gerenciador de Arquivos do Android**
1. Baixe um gerenciador de arquivos (ex: FX File Explorer)
2. Copie os arquivos MPQ para o dispositivo
3. Mova os arquivos para a pasta correta

### Passo 4: Verificar os Arquivos

1. Abra o app DevilutionX novamente
2. Clique em **"Verificar novamente"**
3. O app deve encontrar os arquivos MPQ agora
4. Clique em **"Iniciar"**

---

## 🎮 Parte 5: Como Jogar no Android

### Controles de Acessibilidade

Se você habilitou `SCREEN_READER_INTEGRATION`, o jogo terá recursos especiais:

#### Navegação por Gestos

**Gesto Deslizar para Esquerda (Swipe Left):**
- Navega para a **opção anterior** do menu
- Anúncia o item atual em voz alta

**Gesto Deslizar para Direita (Swipe Right):**
- Navega para a **próxima opção** do menu
- Anúncia o item atual em voz alta

**Dois Toques (Double Tap):**
- Confirma ou **entra na opção** selecionada
- Ativa a seleção atual

#### Controles Virtuais

O jogo também mostra controles virtuais na tela:

```
┌─────────────────────────────────────┐
│  [CHAR] [QUEST] [INV] [MAP]        │ ← Botões de Menu
│                                     │
│         ◉ D-PAD                     │ ← Direcional Virtual
│       ↖ ↑ ↗                         │
│       ← ● →                         │
│       ↙ ↓ ↘                         │
│                                     │
│  [STAND]  [ATTACK] [SPELL] [CANCEL] │ ← Botões de Ação
│                                     │
│  [❤️] [💧]                          │ ← Poções de Vida/Mana
└─────────────────────────────────────┘
```

### Controles Básicos

| Ação | Controle |
|-------|----------|
| Mover personagem | D-PAD virtual ou toque na tela |
| Atacar | Botão ATTACK |
| Abrir inventário | Botão INV |
| Falar magia | Botão SPELL |
| Menu de pausa | Botão voltar do Android |
| Interagir | Toque no objeto/monstro |

### Ativar Acessibilidade

1. **Desative o TalkBack** temporariamente (gestos do jogo entram conflito)
2. O jogo detectará automaticamente que TalkBack está desativado
3. **Modo acessível será ativado automaticamente** ✅
4. Use gestos para navegar nos menus

---

## ⚠️ Solução de Problemas

### Problema: "Não foi possível encontrar DIABDAT.MPQ"

**Solução:**
1. Verifique se copiou os arquivos para a pasta CORRETA
2. O caminho deve ser: `/Android/data/org.diasurgical.devilutionx/files/`
3. Use um gerenciador de arquivos para verificar se os arquivos estão lá
4. Certifique-se de **abrir o app uma vez primeiro** antes de copiar os arquivos

### Problema: Build falha com erro de CMake

**Solução:**
```bash
# Limpar o build
cd android-project
./gradlew clean

# Remover pasta .cxx
rm -rf .cxx

# Tentar novamente
./gradlew assembleDebug
```

### Problema: APK não instala

**Solução:**
1. Desinstale qualquer versão anterior do app
2. Habilite **"Instalar de fontes desconhecidos"** nas configurações do Android
3. Verifique se há espaço suficiente no dispositivo

### Problema: Jogos trava ou fecha

**Solução:**
1. Verifique se copiou TODOS os arquivos MPQ necessários
2. Certifique-se de que `devilutionx.mpq` está presente
3. Tente reiniciar o dispositivo
4. Verifique os logs em: `adb logcat | grep devilutionx`

### Problema: Fala em inglês em vez de português

**Solução:**
1. Acesse **Configurações do Android → Idioma e Entrada**
2. Configure **Idioma** para **Português (Brasil)**
3. Reinicie o app
4. O TTS do Android agora deve usar a voz em português

---

## 📊 Estrutura de Arquivos no Android

```
Android/data/org.diasurgical.devilutionx/files/
├── DIABDAT.MPQ          (ou spawn.mpq) ← OBRIGATÓRIO
├── devilutionx.mpq      (recomendado) ← RECOMENDADO
├── hellfire.mpq         (opcional - expansão)
├── hfmonk.mpq           (opcional - expansão)
├── hfmusic.mpq          (opcional - expansão)
├── hfvoice.mpq          (opcional - expansão)
└── fonts.mpq            (opcional - idiomas asiáticos)
```

---

## 🎯 Checklist Final

- [ ] Clonou o repositório
- [ ] Habilitou `SCREEN_READER_INTEGRATION=ON`
- [ ] Compilou o APK com sucesso
- [ ] Instalou o APK no Android
- [ ] Abriu o app uma vez (para criar pastas)
- [ ] Copiou `DIABDAT.MPQ` ou `spawn.mpq`
- [ ] Copiou `devilutionx.mpq` (recomendado)
- [ ] Copiou arquivos Hellfire (se aplicável)
- [ ] Arquivos estão em `/Android/data/org.diasurgical.devilutionx/files/`
- [ ] App encontrou os arquivos MPQ
- [ ] Jogo iniciou com sucesso!

---

## 🎮 Divirta-se!

Agora você está pronto para jogar Diablo Access no Android com:

✅ **Text-to-Speech em português brasileiro**
✅ **Navegação por gestos intuitivos**
✅ **Anúncios de itens e monstros**
✅ **Sistema de tracking por voz**
✅ **Alertas de HP baixa**

**Boa sorte e divirta-se!** 🎮🎵
