package com.example.everydayenglish.navigation

import android.util.Log
import androidx.compose.runtime.mutableStateListOf

//仿制路由
class CustomNavController(startRoute: String){
    val stack = mutableStateListOf(startRoute)

    val currentRoute: String get() = stack.last()
    val previousRoute: String? get() = stack.getOrNull(stack.size - 2)
    val canGoBack: Boolean get() = stack.size > 1

    fun navigate(route: String) {
        if (stack.isEmpty() || stack.last() != route) stack.add(route)
        Log.d("NavController", "navigate($route) -> stack=$stack")
    }

    fun popBack(): Boolean {
        val before = stack.toList()
        val result = if (canGoBack) { stack.removeAt(stack.size - 1); true } else false
        Log.d("NavController", "popBack() before=$before -> stack=$stack (result=$result)")
        return result
    }

    fun popBackTo(route: String) {
        val before = stack.toList()
        while (stack.last() != route && stack.size > 1) stack.removeAt(stack.size - 1)
        Log.d("NavController", "popBackTo($route) before=$before -> stack=$stack")
    }
    fun popRoute(route: String) {
        val before = stack.toList()
        val index = stack.indexOfLast { it == route }
        if (index >= 0) stack.removeAt(index)
        Log.d("NavController", "popRoute($route) before=$before -> stack=$stack")
    }
}