package com.example.everydayenglish.adaptiveEngine

import com.example.everydayenglish.data.entity.ExerciseRecord
import com.example.everydayenglish.data.entity.ReferenceAnswer
import kotlin.math.ln
import kotlin.math.sqrt

class SwUcbBandit(
    val windowSize: Int = 30,
    val explorationC: Double = 2.0
) {
    private val arms: MutableMap<TenseCategory, ArmState> =
        TenseCategory.entries.associateWith { ArmState(category = it) }
            .toMutableMap()

    var totalPulls: Int = 0

    fun selectArm(): TenseCategory {
        totalPulls++
        val unexplored = arms.values.filter { it.count() == 0 }
        if (unexplored.isNotEmpty()) return unexplored.random().category

        return arms.values.maxByOrNull { arm ->
            val mu = 1.0 - arm.averageAccuracy()
            val n = arm.count()
            val explorationBonus = explorationC *
                    sqrt(ln(minOf(totalPulls, windowSize).toDouble()) / n)
            mu + explorationBonus
        }!!.category
    }

    // accuracy: 0.0~1.0
    fun update(category: TenseCategory, accuracy: Double, timestamp: Long) {
        val arm = arms[category] ?: return
        arm.window.addLast(Pair(timestamp, accuracy))
        while (arm.window.size > windowSize) {
            arm.window.removeFirst()
        }
    }

    fun getArmStats(): Map<TenseCategory, Pair<Double, Int>> =
        arms.mapValues { (_, arm) ->
            Pair(arm.averageAccuracy(), arm.count())
        }
}