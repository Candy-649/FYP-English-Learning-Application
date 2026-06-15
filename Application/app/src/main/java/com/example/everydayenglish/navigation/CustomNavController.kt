package com.example.everydayenglish.navigation

import androidx.compose.runtime.mutableStateListOf

//仿制路由
class CustomNavController(startRoute: String){
    val stack = mutableStateListOf(startRoute)

    val currentRoute: String get() = stack.last()
    val previousRoute: String? get() = stack.getOrNull(stack.size - 2)
    val canGoBack: Boolean get() = stack.size > 1

    fun navigate(route: String) {
        if (stack.isEmpty() || stack.last() != route) stack.add(route)
    }

    fun popBack(): Boolean {
        return if (canGoBack) { stack.removeLast(); true } else false
    }

    fun popBackTo(route: String) {
        while (stack.last() != route && stack.size > 1) stack.removeLast()
    }
}