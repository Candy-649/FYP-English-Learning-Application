package com.example.everydayenglish.adaptiveEngine

import com.example.everydayenglish.data.entity.ExerciseRecord
import com.example.everydayenglish.data.entity.ReferenceAnswer
import kotlin.math.ln
import kotlin.math.sqrt

class SwUcbBandit(
    private val windowSize: Int = 30,
    private val explorationC: Double = 2.0
) {
    private val arms: Map<TenseCategory, ArmState> =
        TenseCategory.entries.associateWith { ArmState(category = it) }
            .toMutableMap()

    private var totalPulls: Int = 0

    fun selectArm(): TenseCategory {
        val unexplored = arms.values.filter { it.count() == 0 }
        if (unexplored.isNotEmpty()) return unexplored.random().category

        totalPulls++

        return arms.values.maxByOrNull { arm ->
            val mu = arm.successRate()
            val n = arm.count()
            val explorationBonus = explorationC *
                    sqrt(ln(minOf(totalPulls, windowSize).toDouble()) / n)
            mu + explorationBonus
        }!!.category
    }

    fun update(category: TenseCategory, isCorrect: Boolean, timestamp: Long) {
        val arm = arms[category] ?: return

        arm.window.addLast(Pair(timestamp, isCorrect))
        while (arm.window.size > windowSize) {
            arm.window.removeFirst()
        }
    }
    fun restoreFromRecords(records: List<ExerciseRecord>, getReferenceAnswer: suspend (Int) -> ReferenceAnswer?) {
    }

    fun getArmStats(): Map<TenseCategory, Pair<Double, Int>> =
        arms.mapValues { (_, arm) ->
            Pair(arm.successRate(), arm.count())
        }
}