package com.dicerpg.mvp

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sqrt
import kotlin.random.Random

object DiceRoller {
    data class Limits(val maxRollTimes: Int = 10_000, val maxDiceSides: Int = 10_000)

    data class RollResult(
        val expression: String,
        val total: Double,
        val rendered: String,
        val errors: List<String>,
    )

    private enum class TokenType { NUMBER, IDENT, SYMBOL, EOF }
    private data class Token(val type: TokenType, val text: String)

    private data class EvalContext(
        val limits: Limits,
        var rollsDone: Int = 0,
        val errors: MutableList<String> = mutableListOf(),
    )

    private data class EvalValue(
        val number: Double,
        val detail: String,
        val high: Double? = null,
        val low: Double? = null,
    )

    fun roll(expr: String, limits: Limits = Limits()): RollResult {
        val parser = Parser(tokenize(expr), limits)
        return try {
            val value = parser.parseExpression()
            parser.expect(TokenType.EOF)
            RollResult(expr, value.number, "${value.detail} = ${format(value.number)}", parser.ctx.errors)
        } catch (e: Exception) {
            RollResult(expr, 0.0, "ERRO", parser.ctx.errors + (e.message ?: "erro desconhecido"))
        }
    }

    private fun tokenize(input: String): List<Token> {
        val src = input.replace(" ", "")
        val out = mutableListOf<Token>()
        var i = 0
        while (i < src.length) {
            val c = src[i]
            when {
                c.isDigit() -> {
                    var j = i
                    while (j < src.length && (src[j].isDigit() || src[j] == '.')) j++
                    out += Token(TokenType.NUMBER, src.substring(i, j))
                    i = j
                }
                c.isLetter() || c == '%' -> {
                    var j = i
                    while (j < src.length && (src[j].isLetterOrDigit() || src[j] == '.' || src[j] == '%')) j++
                    out += Token(TokenType.IDENT, src.substring(i, j))
                    i = j
                }
                else -> {
                    val two = if (i + 1 < src.length) src.substring(i, i + 2) else ""
                    val three = if (i + 2 < src.length) src.substring(i, i + 3) else ""
                    val op = when {
                        three in listOf("!!p", "...") -> three
                        two in listOf("<=", ">=", "!=", "==", "..", "!!", "kh", "kl", "km", "dh", "dl", "dm", "ro", "cs", "cf") -> two
                        else -> c.toString()
                    }
                    out += Token(TokenType.SYMBOL, op)
                    i += op.length
                }
            }
        }
        out += Token(TokenType.EOF, "")
        return out
    }

    private class Parser(private val tokens: List<Token>, limits: Limits) {
        var pos = 0
        val ctx = EvalContext(limits)

        fun parseExpression(): EvalValue = parseComparison()

        private fun parseComparison(): EvalValue {
            var left = parseAddSub()
            while (peekSymbol("<") || peekSymbol(">") || peekSymbol("<=") || peekSymbol(">=") || peekSymbol("=") || peekSymbol("==") || peekSymbol("!=")) {
                val op = advance().text
                val right = parseAddSub()
                val r = compare(left.number, op, right.number)
                left = EvalValue(if (r) 1.0 else 0.0, "(${left.detail}$op${right.detail})")
            }
            return left
        }

        private fun parseAddSub(): EvalValue {
            var left = parseMulDiv()
            while (peekSymbol("+") || peekSymbol("-")) {
                val op = advance().text
                val right = if (peekIdent("H") || peekIdent("L")) {
                    val x = advance().text
                    if (x == "H") EvalValue(left.high ?: left.number, "H") else EvalValue(left.low ?: left.number, "L")
                } else parseMulDiv()
                left = if (op == "+") combine(left, right, left.number + right.number, "+") else combine(left, right, left.number - right.number, "-")
            }
            return left
        }

        private fun parseMulDiv(): EvalValue {
            var left = parseUnary()
            while (peekSymbol("*") || peekSymbol("/")) {
                val op = advance().text
                val right = if (peekIdent("H") || peekIdent("L")) {
                    val x = advance().text
                    if (x == "H") EvalValue(left.high ?: left.number, "H") else EvalValue(left.low ?: left.number, "L")
                } else parseUnary()
                left = if (op == "*") combine(left, right, left.number * right.number, "*") else combine(left, right, left.number / right.number, "/")
            }
            return left
        }

        private fun parseUnary(): EvalValue {
            if (peekSymbol("-")) {
                advance(); val v = parseUnary(); return EvalValue(-v.number, "(-${v.detail})")
            }
            return parsePrimary()
        }

        private fun parsePrimary(): EvalValue {
            if (peek(TokenType.NUMBER)) { val t = advance(); return EvalValue(t.text.toDouble(), t.text) }
            if (peekSymbol("(")) {
                advance(); val v = parseExpression(); expectSymbol(")"); return v
            }
            if (peekSymbol("{")) return parseGroup()
            if (peek(TokenType.IDENT) && lookAheadIs("(")) return parseFunction()
            return parseDiceOrIdentifier()
        }

        private fun parseFunction(): EvalValue {
            val name = expect(TokenType.IDENT).text.lowercase()
            expectSymbol("(")
            val arg = parseExpression()
            expectSymbol(")")
            val v = when (name) {
                "abs" -> abs(arg.number)
                "ceil" -> ceil(arg.number)
                "floor" -> floor(arg.number)
                "round" -> round(arg.number)
                "sqrt" -> sqrt(arg.number)
                else -> {
                    ctx.errors += "Função custom '$name' não registrada"
                    arg.number
                }
            }
            return EvalValue(v, "$name(${arg.detail})")
        }

        private fun parseGroup(): EvalValue {
            expectSymbol("{")
            val values = mutableListOf<EvalValue>()
            values += parseExpression()
            while (peekSymbol(",")) { advance(); values += parseExpression() }
            if (peekSymbol("...")) {
                advance(); val times = parseExpression().number.toInt()
                expectSymbol("}")
                val expanded = (1..times).map { values[0].number }
                return EvalValue(expanded.sum(), "{${expanded.joinToString(",")}}")
            }
            expectSymbol("}")
            var nums = values.map { it.number }
            if (peekSymbol("kh") || peekSymbol("kl") || peekSymbol("km") || peekSymbol("dh") || peekSymbol("dl") || peekSymbol("dm")) {
                val op = advance().text
                val n = if (peek(TokenType.NUMBER)) advance().text.toInt() else 1
                nums = applyKeepDrop(op, nums, n)
            }
            return EvalValue(nums.sum(), "{${nums.joinToString(",")}}", nums.maxOrNull(), nums.minOrNull())
        }

        private fun parseDiceOrIdentifier(): EvalValue {
            var qty = 1
            var qtyDetail = "1"
            if (peek(TokenType.NUMBER) && peekSymbol("d", offset = 1)) {
                qty = round(advance().text.toDouble()).toInt().coerceAtLeast(1)
                qtyDetail = qty.toString()
            }
            if (peekSymbol("d") || peekIdent("d")) {
                if (peekIdent("d")) advance() else expectSymbol("d")
                return parseDice(qty, qtyDetail)
            }
            val t = advance()
            if (t.type == TokenType.IDENT && t.text == "d%") return rollDice(1, 100, "d%")
            if (t.type == TokenType.IDENT && t.text.startsWith("df")) return rollFudge(t.text)
            throw IllegalArgumentException("Token inesperado: ${t.text}")
        }

        private fun parseDice(qty: Int, qtyDetail: String): EvalValue {
            val sideToken = advance()
            val sides = when {
                sideToken.type == TokenType.NUMBER -> sideToken.text.toInt()
                sideToken.type == TokenType.IDENT && sideToken.text == "%" -> 100
                sideToken.type == TokenType.IDENT && sideToken.text.startsWith("f") -> return rollFudge("d${sideToken.text}", qty)
                else -> throw IllegalArgumentException("Lados inválidos")
            }
            var rolls = MutableList(qty) { oneRoll(sides) }
            var detail = "${qtyDetail}d$sides"

            // reroll
            if (peekSymbol("r") || peekSymbol("ro")) {
                val once = advance().text == "ro"
                val cond = parseOptionalCompareCond()
                rolls = rolls.map { rerollValue(it, sides, cond, once) }.toMutableList()
                detail += if (once) "ro" else "r"
            }

            // explode
            if (peekSymbol("!") || peekSymbol("!!") || peekSymbol("!p") || peekSymbol("!!p")) {
                val mode = advance().text
                val cond = parseOptionalCompareCond(default = ">=")
                rolls = applyExplode(rolls, sides, mode, cond).toMutableList()
                detail += mode
            }

            // keep/drop
            if (peekSymbol("kh") || peekSymbol("kl") || peekSymbol("km") || peekSymbol("dh") || peekSymbol("dl") || peekSymbol("dm")) {
                val op = advance().text
                val n = if (peek(TokenType.NUMBER)) advance().text.toInt() else 1
                rolls = applyKeepDrop(op, rolls.map { it.toDouble() }, n).map { it.toInt() }.toMutableList()
                detail += "$op$n"
            }

            val sum = rolls.sum().toDouble()
            return EvalValue(sum, "$detail[${rolls.joinToString(",")}]", rolls.maxOrNull()?.toDouble(), rolls.minOrNull()?.toDouble())
        }

        private fun rollDice(qty: Int, sides: Int, label: String): EvalValue {
            val rolls = (1..qty).map { oneRoll(sides) }
            return EvalValue(rolls.sum().toDouble(), "$label[${rolls.joinToString(",")}]", rolls.maxOrNull()?.toDouble(), rolls.minOrNull()?.toDouble())
        }

        private fun rollFudge(token: String, qty: Int = 1): EvalValue {
            val map = when {
                token.contains(".1") -> listOf(-1, 0)
                token.contains(".2") -> listOf(-1, 0, 1)
                else -> listOf(-1, 0, 1)
            }
            val rolls = (1..qty).map { map.random() }
            return EvalValue(rolls.sum().toDouble(), "$token[${rolls.joinToString(",")}]", rolls.maxOrNull()?.toDouble(), rolls.minOrNull()?.toDouble())
        }

        private fun applyKeepDrop(op: String, nums: List<Double>, n: Int): List<Double> {
            val k = n.coerceAtMost(nums.size)
            return when (op) {
                "kh" -> nums.sortedDescending().take(k)
                "kl" -> nums.sorted().take(k)
                "km" -> nums.sorted().drop((nums.size - k) / 2).take(k)
                "dh" -> nums.sortedDescending().drop(k)
                "dl" -> nums.sorted().drop(k)
                "dm" -> nums.sorted().filterIndexed { i, _ -> i < (nums.size - k) / 2 || i >= (nums.size + k) / 2 }
                else -> nums
            }
        }

        private fun parseOptionalCompareCond(default: String? = null): Pair<String, Double> {
            val op = when {
                peekSymbol("<=") || peekSymbol(">=") || peekSymbol("!=") || peekSymbol("=") || peekSymbol("==") || peekSymbol("<") || peekSymbol(">") -> advance().text
                else -> default ?: return Pair("=", Double.NaN)
            }
            val v = parseUnary().number
            return Pair(op, v)
        }

        private fun rerollValue(current: Int, sides: Int, cond: Pair<String, Double>, once: Boolean): Int {
            var x = current
            while (compare(x.toDouble(), cond.first, cond.second)) {
                x = oneRoll(sides)
                if (once) break
            }
            return x
        }

        private fun applyExplode(initial: List<Int>, sides: Int, mode: String, cond: Pair<String, Double>): List<Int> {
            val out = mutableListOf<Int>()
            for (r in initial) {
                var cur = r
                var acc = if (mode.startsWith("!!")) r else 0
                out += if (mode.startsWith("!!")) 0 else r
                while (compare(cur.toDouble(), cond.first, cond.second)) {
                    var next = oneRoll(sides)
                    if (mode.contains("p")) next -= 1
                    if (mode.startsWith("!!")) acc += next else out += next
                    cur = next
                }
                if (mode.startsWith("!!")) out[out.lastIndex] = acc
            }
            return out
        }

        private fun oneRoll(sides: Int): Int {
            require(sides in 1..ctx.limits.maxDiceSides) { "Lados fora do limite (${ctx.limits.maxDiceSides})" }
            ctx.rollsDone++
            require(ctx.rollsDone <= ctx.limits.maxRollTimes) { "Quantidade de rolagens excedeu limite (${ctx.limits.maxRollTimes})" }
            return Random.nextInt(1, sides + 1)
        }

        private fun combine(a: EvalValue, b: EvalValue, n: Double, op: String) = EvalValue(n, "(${a.detail}$op${b.detail})", a.high, a.low)
        private fun compare(a: Double, op: String, b: Double): Boolean = when (op) {
            "<" -> a < b; ">" -> a > b; "<=" -> a <= b; ">=" -> a >= b; "=", "==" -> a == b; "!=" -> a != b; else -> false
        }

        fun expect(type: TokenType): Token = if (peek(type)) advance() else throw IllegalArgumentException("Esperado $type")
        fun expectSymbol(symbol: String): Token = if (peekSymbol(symbol)) advance() else throw IllegalArgumentException("Esperado '$symbol'")
        private fun peek(type: TokenType, offset: Int = 0): Boolean = tokens.getOrNull(pos + offset)?.type == type
        private fun peekSymbol(symbol: String, offset: Int = 0): Boolean = tokens.getOrNull(pos + offset)?.let { it.type == TokenType.SYMBOL && it.text.lowercase() == symbol.lowercase() } == true
        private fun peekIdent(id: String, offset: Int = 0): Boolean = tokens.getOrNull(pos + offset)?.let { it.type == TokenType.IDENT && it.text.lowercase() == id.lowercase() } == true
        private fun lookAheadIs(symbol: String): Boolean = peekSymbol(symbol, 1)
        private fun advance(back: Int = 0): Token { if (back < 0) return tokens[pos + back]; return tokens[pos++] }
    }

    private fun format(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else "%.2f".format(v)
}
