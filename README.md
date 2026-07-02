# 🐉 AventuraRPG — Linguagem de Descrição de Campanhas de RPG

> Uma linguagem de domínio específico (DSL) para descrever campanhas de RPG em português, com compilador que interpreta e executa aventuras interativas no terminal.
> 
> Projeto desenvolvido para a disciplina de **Construção de Compiladores** (Trabalho 6) - Prof. Daniel Lucrédio.
## 📋 Índice

- [Sobre a Linguagem](#sobre-a-linguagem)
- [Estrutura da Linguagem](#estrutura-da-linguagem)
- [Análise Léxica e Sintática](#análise-léxica-e-sintática)
- [Análise Semântica](#análise-semântica)
- [Interpretação](#interpretação)
- [Como Compilar o Compilador](#como-compilar-o-compilador)
- [Como Usar](#como-usar)
- [Casos de Teste](#casos-de-teste)
- [Estrutura do Projeto](#estrutura-do-projeto)

---

## Sobre a Linguagem

**AventuraRPG** é uma DSL que permite descrever campanhas de RPG de forma declarativa em português. No arquivo `.txt` escrito em AventuraRPG descreve personagens, itens, encontros e missões — e o compilador transforma isso em uma aventura interativa no terminal, com batalhas por turnos e escolhas do jogador.

**Para que serve:**
- Criar aventuras de RPG sem programar
- Aprender conceitos de compiladores com um domínio lúdico
- Gerar sessões de RPG solo

---

## Estrutura da Linguagem

Um programa AventuraRPG é composto por uma **campanha** contendo quatro tipos de declarações:

```
campanha "Nome da Campanha" {
    personagem NomeID { ... }
    item "Nome do Item" { ... }
    encontro "Nome do Encontro" { ... }
    missao "Nome da Missao" { ... }
}
```

### Personagem

```
personagem Aragorn {
    classe: guerreiro          // guerreiro | mago | ladrao | clerigo | patrulheiro
    nivel: 5
    pv: 80                     // pontos de vida (max = nivel x 20)
    atributos {
        forca: 16,
        destreza: 12,
        constituicao: 14,
        inteligencia: 10,
        sabedoria: 10,
        carisma: 8
    }
    inventario { "Espada Longa", "Pocao de Cura" }
}
```

### Item

```
item "Pocao de Cura" {
    tipo: consumivel           // consumivel | arma | armadura | item_chave | misc
    efeito: curar(20)          // curar(N) | danificar(N) | fortalecer(attr, N) | desbloquear("local")
    valor: 10
}
```

### Encontro

```
encontro "Covil dos Goblins" {
    inimigos { goblin x3, orc x1 }
    dificuldade: medio         // facil | medio | dificil | letal
    recompensa {
        "Pocao de Cura",
        ouro: 50,
        xp: 200
    }
}
```

### Missão

```
missao "A Grande Busca" {
    requer { "Missao Anterior" }
    passos {
        falar_com Eldrin
        entrar "Covil dos Goblins"
        coletar "Espada Lendaria"
        usar "Chave do Portal"
        retornar "Cidade de Pedra"
    }
    recompensa {
        ouro: 500,
        xp: 1000
    }
}
```

### Comentários

```
// comentário de linha

/* comentário
   de bloco */
```

---

## Análise Léxica e Sintática

O compilador usa **ANTLR 4** para gerar automaticamente o analisador léxico e sintático a partir da gramática `AventuraRPG.g4`.

### Tokens reconhecidos

| Categoria | Exemplos |
|---|---|
| Palavras-chave de estrutura | `campanha`, `personagem`, `item`, `encontro`, `missao` |
| Palavras-chave de campo | `classe`, `nivel`, `pv`, `atributos`, `inventario`, `tipo`, `efeito`, `valor` |
| Classes | `guerreiro`, `mago`, `ladrao`, `clerigo`, `patrulheiro` |
| Tipos de item | `consumivel`, `arma`, `armadura`, `item_chave`, `misc` |
| Efeitos | `curar`, `danificar`, `fortalecer`, `desbloquear` |
| Dificuldades | `facil`, `medio`, `dificil`, `letal` |
| Atributos | `forca`, `destreza`, `inteligencia`, `sabedoria`, `carisma`, `constituicao` |
| Ações de passo | `entrar`, `coletar`, `retornar`, `falar_com`, `usar` |
| Literais | `ID`, `INT`, `STRING` (`"texto"`), `MULT` (`x3`) |
| Pontuação | `{`, `}`, `(`, `)`, `:`, `,` |
| Ignorados | espaços, quebras de linha, comentários `//` e `/* */` |

### Erros léxicos detectados

- Caracteres inválidos no identificador (ex: `Her@i`)
- Strings não fechadas

### Erros sintáticos detectados

- Ausência de identificador após palavra-chave
- Efeito sem parênteses e argumentos
- Nível de dificuldade com valor não reconhecido
- Bloco sem chave de fechamento

---

## Análise Semântica

O compilador realiza **duas passagens** sobre a árvore sintática:

1. **Primeira passagem** — registra todos os nomes declarados na tabela de símbolos
2. **Segunda passagem** — aplica as verificações semânticas

### Verificações implementadas

| # | Verificação | Exemplo de erro |
|---|---|---|
| 1 | **Unicidade de nomes** — nenhum personagem, item, encontro ou missão pode ser declarado duas vezes | `Linha 22: nome 'Aragorn' ja foi declarado na linha 10` |
| 2 | **PV coerente com nível** — PV máximo = nível × 20 | `Linha 8: personagem 'Heroi' tem pv=200 mas nivel 3 permite no maximo 60` |
| 3 | **Itens em recompensas declarados** — itens referenciados em `recompensa { }` devem existir | `Linha 35: item 'Amuleto Raro' usado em recompensa nao foi declarado` |
| 4 | **Encontros em passos declarados** — `entrar "X"` requer que `X` seja um encontro declarado | `Linha 48: passo 'entrar "Sala Secreta"' referencia um encontro nao declarado` |
| 5 | **Itens no inventário declarados** — itens no `inventario { }` do personagem devem existir | `Linha 12: personagem 'Heroi' possui 'Espada Magica' no inventario, mas esse item nao foi declarado` |

---

## Interpretação

Após análise sem erros, o compilador **interpreta** o programa diretamente, executando uma aventura interativa no terminal:

1. **Seleção de personagem** — o jogador escolhe entre os personagens declarados e informa seu nome
2. **Exibição da ficha** — atributos, classe, nível e inventário inicial
3. **Execução das missões** — em ordem de declaração, cada passo é processado:
   - `entrar` → batalha por turnos com menu de ação (atacar / defender / usar item)
   - `coletar` → jogador decide pegar ou ignorar o item
   - `falar_com` → diálogo com NPC e escolha de resposta
   - `usar` → aplica efeito do item (cura, fortalecimento)
   - `retornar` → transição de local
4. **Tela final** — vitória com resumo (XP, ouro, itens coletados) ou derrota

---

## Como Compilar o Compilador

### Pré-requisitos

- Java 25
- Maven 3.8+

### Compilar

```bash
cd AventuraRPG
mvn package -q
```

O jar executável é gerado em:
```
AventuraRPG/target/AventuraRPG-1.0-SNAPSHOT-jar-with-dependencies.jar
```

---

## Como Usar

```bash
java -jar target/AventuraRPG-1.0-SNAPSHOT-jar-with-dependencies.jar <arquivo.txt>
```

**Exemplo:**
```bash
java -jar AventuraRPG/target/AventuraRPG-1.0-SNAPSHOT-jar-with-dependencies.jar campanha_sem_erros.txt
```

Se houver erros léxicos, sintáticos ou semânticos, eles são impressos no terminal e a execução para. Caso contrário, a aventura inicia interativamente.

---

## Casos de Teste

O projeto inclui 4 arquivos de teste prontos:

| Arquivo | Propósito | Resultado esperado |
|---|---|---|
| `campanha_sem_erros.txt` | Campanha válida completa com 3 personagens, 5 itens, 3 encontros e 3 missões | Aventura interativa executada com sucesso |
| `campanha_com_erros.txt` | 5 erros semânticos propositais (duplicidade, PV inválido, itens não declarados, encontro não declarado) | Lista de 5 erros semânticos + `Fim da compilacao` |
| `teste_lexico.txt` | Erro léxico: caractere `@` inválido em identificador | Erro léxico na linha indicada |
| `teste_sintatico.txt` | Erros sintáticos: falta de ID, efeito sem argumentos, dificuldade inválida | Erros sintáticos nas linhas indicadas |

### Executar todos os testes

```bash
JAR=target/AventuraRPG-1.0-SNAPSHOT-jar-with-dependencies.jar

echo "=== CAMPANHA SEM ERROS (deve iniciar o jogo) ==="
echo "" | java -jar $JAR AventuraRPG/testes/campanha_sem_erros.txt

echo "=== CAMPANHA COM ERROS SEMANTICOS ==="
java -jar $JAR AventuraRPG/testes/campanha_com_erros.txt

echo "=== ERRO LEXICO ==="
java -jar $JAR AventuraRPG/testes/teste_lexico.txt

echo "=== ERROS SINTATICOS ==="
java -jar $JAR AventuraRPG/testes/teste_sintatico.txt
```

---

## Estrutura do Projeto

```
AventuraRPG/
├── pom.xml                          # Configuração Maven (ANTLR 4.12.0, Java 25)
├── campanha_sem_erros.txt           # Caso de teste: campanha válida
├── campanha_com_erros.txt           # Caso de teste: erros semânticos
├── teste_lexico.txt                 # Caso de teste: erro léxico
├── teste_sintatico.txt              # Caso de teste: erros sintáticos
└── src/main/
    ├── antlr4/.../aventurarpg/
    │   └── AventuraRPG.g4           # Gramática da linguagem
    └── java/.../aventurarpg/
        ├── Principal.java           
        ├── TabelaDeSimbolos.java    
        ├── Escopos.java             
        ├── AventuraRPGSemanticoUtils.java 
        ├── AventuraRPGSemantico.java       
        └── AventuraRPGInterpretador.java 
```

### Fluxo de compilação

```
arquivo.txt
    │
    ▼
AventuraRPGLexer        ← gerado pelo ANTLR a partir de AventuraRPG.g4
    │  tokens
    ▼
AventuraRPGParser       ← gerado pelo ANTLR a partir de AventuraRPG.g4
    │  árvore sintática (AST)
    ▼
AventuraRPGSemantico    ← 2 passagens: registra nomes + verifica regras
    │  AST validada
    ▼
AventuraRPGInterpretador  ← percorre a AST e executa a aventura no terminal
```

---
