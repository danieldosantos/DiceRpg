# DiceRPG Kotlin - Inventário de rolagens implementadas

Este projeto agora possui um motor de rolagem em Kotlin com parser interpretado (não apenas regex), cobrindo as regras pedidas com foco funcional para o app Android (`DiceRoller`).

## Onde está a implementação
- `android-app/app/src/main/java/com/dicerpg/mvp/DiceRoller.kt`

## Regras cobertas

### 1) Núcleo (lexer/parser/interpreter)
- Tokenização própria (`NUMBER`, `IDENT`, `SYMBOL`, `EOF`)
- Parser de expressão com precedência:
  - comparação
  - soma/subtração
  - multiplicação/divisão
  - unário
  - primários (número, função, grupo, dado)
- Resultado estruturado com:
  - total numérico
  - renderização detalhada
  - lista de erros

### 2) Limites de segurança
- `Limits(maxRollTimes, maxDiceSides)`
- Bloqueio de abuso por:
  - excesso de rolagens
  - lados acima do limite

### 3) Sintaxe base de dados
- `NdM` (ex: `2d6`, `1d20`)
- operações combinadas (`1d6+2d10`, `4d6/2d3`)
- comparação (`>=`, `<=`, `>`, `<`, `=`, `==`, `!=`)

### 4) Percentual e Fudge
- `d%` => d100
- `dF`, `dF.1`, `dF.2`

### 5) Explode / compound / penetrate
- `!`, `!!`, `!p`, `!!p`
- compare-point opcional em explode:
  - `!>4`, `!<2`, `!=2`, `!!<=4`, `!p!=4`

### 6) Reroll
- `r` (rerroll contínuo enquanto condição for verdadeira)
- `ro` (rerroll apenas uma vez)
- com condição, ex: `r<3`

### 7) Keep / Drop
- keep: `kh`, `kl`, `km`
- drop: `dh`, `dl`, `dm`
- quantidade opcional: ex. `kh3`, `dl1`
- funciona para rolagens e grupos

### 8) Grupos e repetição
- grupos: `{...}`
- listas com vírgula: `{5,2}`
- repetidor: `{expr...N}`

### 9) Funções matemáticas
- `abs`, `ceil`, `floor`, `round`, `sqrt`
- função desconhecida é reportada em `errors`

### 10) Alto/Baixo em operações (H/L)
- Suporte de `H` e `L` em operações com o termo anterior
- Exemplo: `3d6*H`, `2d10-L`

## Exemplos práticos
- Básico: `2d6+3`, `1d20`
- Multi-termo: `1d6+2d10`, `4d6/2d3`, `3d6*H`
- Percentual/Fudge: `d%`, `4dF.2-L`, `dF.1*2`
- Explode: `4d10!`, `1d2!!`, `1d2!p`, `2d2!!p`
- Explode condicional: `1d6!>1`, `1d2!<2`, `1d3!=2`, `1d6!!>1`
- Keep/Drop: `4d6kh3`, `4d6dl1`, `{5,2}kh`
- Grupo/repetição: `{2d20...10}>=14`
- Funções: `floor(1d20/2)`, `sqrt(9)`

## Observações
- O resultado textual inclui a forma renderizada da rolagem + total final.
- Campos de crítico/sucesso/falha decorados (`cs/cf` visual) não foram expostos como decoração rica na UI ainda; o núcleo foi estruturado para evolução incremental no mesmo arquivo.
