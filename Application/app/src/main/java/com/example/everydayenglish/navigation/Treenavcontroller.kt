package com.example.everydayenglish.navigation

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class NavNode(val route: String, val parent: NavNode? = null) {
    var left: NavNode? = null
    var right: NavNode? = null
    override fun toString(): String = "$route@${System.identityHashCode(this)}"
}

// left/right 永远是具体节点，不可空。两者指向同一个引用 = 单栏语义；
// 指向不同引用 = 双栏语义。调用方靠 === 比较来判断栏数，不再靠字段判空。
data class DualPaneLayout(val left: NavNode, val right: NavNode)

class TreeNavController(startRoute: String) {

    var root: NavNode = NavNode(startRoute)
        private set

    var current: NavNode by mutableStateOf(root)
        private set

    val currentRoute: String get() = current.route
    val canGoBack: Boolean get() = current.parent != null

    // 单纯用来强制触发重组：任何一次树结构变化（哪怕current的引用没变）
    // 都bump一下，dualPaneLayout读它一次，把它变成Compose的依赖项。
    private var version by mutableStateOf(0)
    private fun bump() { version++ }

    val dualPaneLayout: DualPaneLayout
        get() {
            @Suppress("UNUSED_EXPRESSION") version // 建立依赖，读取即可
            val parent = current.parent
            val (l, r) = when {
                parent == null -> (current.left ?: current) to (current.right ?: current)
                parent.left === current -> current to parent
                else -> (parent.left ?: current) to current
            }
            Log.d("TreeNav", "dualPaneLayout: current=$current, parent=$parent -> left=$l, right=$r")
            return DualPaneLayout(left = l, right = r)
        }

    fun navigate(route: String, from: NavNode = current) {
        val targetIsLeft = from.left == null
        val existing = if (targetIsLeft) null else from.right
        val node = if (existing?.route == route) existing else NavNode(route, parent = from).also {
            if (targetIsLeft) from.left = it else from.right = it
        }
        Log.d("TreeNav", "navigate($route) from=$from -> slot=${if (targetIsLeft) "left" else "right"}, node=$node")
        current = node
        bump()
    }

    fun popBack(from: NavNode = current): Boolean {
        val parent = from.parent ?: return false
        if (parent.left === from) parent.left = null
        else if (parent.right === from) parent.right = null
        Log.d("TreeNav", "popBack from=$from -> $parent (pruned)")
        current = parent
        bump()
        return true
    }

    fun popBackTo(route: String, from: NavNode = current) {
        var node = from
        while (node.route != route && node.parent != null) {
            val parent = node.parent!!
            if (parent.left === node) parent.left = null
            else if (parent.right === node) parent.right = null
            Log.d("TreeNav", "  pruned $node from $parent")
            node = parent
        }
        Log.d("TreeNav", "popBackTo($route) -> $node")
        current = node
        bump()
    }

    fun reset(route: String) {
        root = NavNode(route)
        current = root
        bump()
    }
}