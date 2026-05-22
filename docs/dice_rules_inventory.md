# Inventário de regras de rolagem

Este documento lista as regras/sintaxes cobertas nas pastas `dice-typescript-master` e `rpg-dice-roller-master`.

## 1) Regras em `dice-typescript-master`

### Núcleo de parsing/interpretação
- Pipeline completo de **lexer + parser + interpreter + generator** (não só regex).
- Suporte a configuração para limitar quantidade de rolagens (`maxRollTimes`) e lados (`maxDiceSides`).
- Resultado inclui total, erros e renderização detalhada da expressão.

### Sintaxe/base
- Notação base de dado: `NdM` (ex.: `1d20`).
- Expressões aninhadas para quantidade de dados: `(4d4)d20`.
- Quantidade fracionária de dados com arredondamento para inteiro mais próximo (ex.: `(2/5)d6`).

### Operadores matemáticos e comparações
- Operações aritméticas em expressões.
- Operadores condicionais/comparação (`>`, `<`, `>=`, `<=`, etc.) em testes de sucesso/falha e condições.

### Modificadores e regras de dado
- `keep`: manter maior/menor/meio (`kh`, `kl`, `km`) com quantidade opcional (ex.: `kl2`).
- `drop`: descartar maior/menor/meio (`dh`, `dl`, `dm`) com quantidade opcional.
- `explode`: explosão (`!`), compounding (`!!`), penetrating (`!p` e `!!p`).
- `reroll`: rerrolar (`r`) e rerrolar uma vez (`ro`), inclusive com condição (ex.: `r<3`).
- `critical`: marcação/regra de crítico (ex.: `cs`, `cs=6` em specs de geração).
- Sucesso/falha em resultados por condições.

### Funções
- Funções embutidas: `abs`, `ceil`, `floor`, `round`, `sqrt`.
- Funções customizadas registráveis.

### Grupos e repetidores
- Grupos de valores/expressões: `{...}`.
- Repetidor de grupo: `{expr...N}` (ex.: `{2d20kl...10}>=14`).

### Renderização/decoradores
- Decoradores de resultado para `reroll`, `explode`, `drop`, `critical`, `success`, `failure`.
- Decoradores customizáveis (string simples ou par abre/fecha).

---

## 2) Regras em `rpg-dice-roller-master`

### Sintaxe/base
- Notação base: `NdM` e combinações como `2d6+12`.
- Suporte a múltiplos blocos de dados na mesma expressão (ex.: `1d6+2d10`).

### Operadores matemáticos
- Soma, subtração, multiplicação e divisão entre resultados/termos de dados.
- Uso de `H` e `L` em adições/operações (alto/baixo): `+H`, `-L`, `*H`, `/L` etc.

### Percentual e fudge
- Percentual com `d%` (equivalente a `d100`).
- Fudge dice: `dF`, `dF.2`, `dF.1`.

### Explode/compound/penetrate
- Exploding dice: `!`.
- Compounding explode: `!!`.
- Penetrating explode: `!p`.
- Penetrating + compounding: `!!p`.

### Compare point (gatilho de explode/penetrate)
- Condições de gatilho por comparação: `=`, `!=`, `<`, `>`, `<=`, `>=`.
- Exemplos: `!>4`, `!<2`, `!=2`, `!!<=4`, `!p!=4`.

### Parsing suportado
- Regex de notação cobre: `qty? d (sides|%|F(.1|.2)?)` + explode opcional + adições opcionais.
- Adições aceitam número e também `H|L`.

---

## 3) Quais rolagens essas libs cobrem (mapa prático)

### Cobertura comum (as duas cobrem)
- `NdM` básico.
- Operações aritméticas em volta de rolagens.
- Explosão de dado (`!`) e variantes avançadas (`!!`, `!p`, `!!p`).
- Condições/comparações em regras.

### Cobertura forte de `dice-typescript-master`
- Parser/AST completo para expressões complexas.
- Grupos `{}` e repetidores `{...N}`.
- Keep/Drop ricos (`kh/kl/km`, `dh/dl/dm`).
- Reroll (`r`, `ro`, condicional).
- Crítico/sucesso/falha com renderização decorada.
- Funções matemáticas + custom functions.
- Limites de segurança configuráveis (qtd rolagens/lados).

### Cobertura forte de `rpg-dice-roller-master`
- Notação direta e robusta para uso prático clássico.
- `d%`, `dF`, `dF.1`, `dF.2`.
- Uso extenso de `H/L` combinado com `+ - * /`.
- Suíte de testes com muitos cenários de explode/compound/penetrate/compare point.

---

## 4) Exemplos de rolagens cobertas

- Básico: `2d6+3`, `1d20`.
- Multi-termo: `1d6+2d10`, `4d6/2d3`, `3d6*2d10-L`.
- Percentual/Fudge: `d%`, `4dF.2-L`, `dF.1*2`.
- Explode: `4d10!`, `1d2!!`, `1d2!p`, `2d2!!p`.
- Explode condicional: `1d6!>1`, `1d2!<2`, `1d3!=2`, `1d6!!>1`.
- Keep/Drop (principalmente no TypeScript): `4d6kh3`, `4d6dl1`, `{5,2}kh`.
- Grupos/repetição (TypeScript): `{2d20kl...10}>=14`.
- Funções (TypeScript): `floor(1d20/2)`, `sqrt(9)`.

---

## 5) Observação para o próximo passo (implantação no Kotlin)

Para levar isso ao app Kotlin com fidelidade, o melhor caminho é:
1. criar parser/AST (em vez de regex única);
2. implementar avaliador por fases (básico -> explode/reroll -> keep/drop -> grupos/funções);
3. validar com suíte de testes espelhando exemplos acima.
