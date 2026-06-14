package com.example.everydayenglish

import com.example.everydayenglish.adaptiveEngine.SwUcbBandit
import com.example.everydayenglish.adaptiveEngine.TenseCategory
import org.junit.Assert.*
import org.junit.Test

class SwUcbBanditTest {

    // 1. 未探索的 arm 应该被优先选择
    @Test
    fun `unexplored arm is selected first`() {
        val bandit = SwUcbBandit()
        val selected = bandit.selectArm()
        val stats = bandit.getArmStats()
        // 选完之后 totalPulls 应该增加
        assertEquals(1, bandit.totalPulls)
        // 所有 arm 还是未探索状态（update 还没被调用）
        assertTrue(stats.values.all { (_, n) -> n == 0 })
    }

    // 2. update 之后 arm 的数据应该正确记录
    @Test
    fun `update records accuracy correctly`() {
        val bandit = SwUcbBandit()
        bandit.update(TenseCategory.PAST_SIMPLE, 1.0, System.currentTimeMillis())
        val stats = bandit.getArmStats()
        val (mu, n) = stats[TenseCategory.PAST_SIMPLE]!!
        assertEquals(1, n)
        assertEquals(1.0, mu, 0.001)
    }

    // 3. 低准确率的 arm 在充分探索后应该被更频繁选择
    @Test
    fun `low accuracy arm is preferred after exploration`() {
        val bandit = SwUcbBandit(windowSize = 30)
        val now = System.currentTimeMillis()

        // 给所有 arm 都 update 一次，打破未探索状态
        TenseCategory.entries.forEach {
            bandit.update(it, 0.8, now)  // 大家都高准确率
        }
        // 给 PRESENT_SIMPLE 很低的准确率
        repeat(10) {
            bandit.update(TenseCategory.PRESENT_SIMPLE, 0.0, now)
        }
        // 多选几次，PRESENT_SIMPLE 应该出现
        val selections = (1..20).map { bandit.selectArm() }
        assertTrue(selections.contains(TenseCategory.PRESENT_SIMPLE))
    }

    // 4. window size 限制：超出 windowSize 的旧记录应该被移除
    @Test
    fun `window size is respected`() {
        val bandit = SwUcbBandit(windowSize = 5)
        val now = System.currentTimeMillis()
        repeat(10) {
            bandit.update(TenseCategory.PAST_SIMPLE, 1.0, now)
        }
        val (_, n) = bandit.getArmStats()[TenseCategory.PAST_SIMPLE]!!
        assertEquals(5, n)
    }

    // 5. TenseCategory.fromTenseString 解析
    @Test
    fun `fromTenseString parses correctly`() {
        assertEquals(TenseCategory.PAST_SIMPLE, TenseCategory.fromTenseString("Past Simple"))
        assertEquals(TenseCategory.PRESENT_PERFECT, TenseCategory.fromTenseString("Present Perfect"))
        assertNull(TenseCategory.fromTenseString("Invalid Tense"))
    }
}