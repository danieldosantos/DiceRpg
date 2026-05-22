package com.dicerpg.mvp

import kotlin.random.Random

object DiceRoller {
    data class RollResult(val expression: String, val rolls: List<Int>, val modifier: Int, val total: Int)

    fun roll(expr: String): RollResult {
        val clean = expr.lowercase().replace(" ", "")
        val regex = Regex("(\\d*)d(\\d+)([+-]\\d+)?")
        val match = regex.matchEntire(clean) ?: throw IllegalArgumentException("Formato inválido. Use NdM+K")
        val count = match.groupValues[1].ifEmpty { "1" }.toInt()
        val sides = match.groupValues[2].toInt()
        val modifier = match.groupValues[3].ifEmpty { "0" }.toInt()
        require(count in 1..100) { "Quantidade de dados inválida" }
        require(sides in 2..1000) { "Lados inválidos" }

        val rolls = (1..count).map { Random.nextInt(1, sides + 1) }
        return RollResult(expr, rolls, modifier, rolls.sum() + modifier)
    }
}
