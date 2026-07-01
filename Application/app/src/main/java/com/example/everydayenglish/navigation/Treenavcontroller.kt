package com.example.everydayenglish.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// A navigation node. Each node has at most two children (left/right slot), plus a parent
// pointer for popBack(). A plain linear stack is just the degenerate case where every node
// only ever uses one slot.
class NavNode(val route: String, val parent: NavNode? = null) {
    var left: NavNode? = null
    var right: NavNode? = null

    override fun toString(): String = "NavNode($route)"
}

// What the two panes should show for a given `current` node - pure function of tree shape,
// no separate history to maintain:
//   - current is its parent's left child  -> left=current, right=parent
//   - current is its parent's right child -> left=parent.left (may be null), right=current
//   - current is the root (no parent)     -> left=current.left (may be null), right=current
data class DualPaneLayout(val left: String?, val right: String)

// Not wired into the app yet - standalone, so it can be built up and tested on its own
// before replacing CustomNavController.
class TreeNavController(startRoute: String) {

    var root: NavNode = NavNode(startRoute)
        private set

    var current: NavNode by mutableStateOf(root)
        private set

    val currentRoute: String get() = current.route
    val canGoBack: Boolean get() = current.parent != null

    val dualPaneLayout: DualPaneLayout
        get() {
            val parent = current.parent
            return when {
                parent == null -> DualPaneLayout(left = current.left?.route, right = current.route)
                parent.left === current -> DualPaneLayout(left = current.route, right = parent.route)
                else -> DualPaneLayout(left = parent.left?.route, right = current.route)
            }
        }

    // Slot is decided automatically, not passed in: if `current` has no left child yet,
    // the new node becomes the left child; if it already has one, the new node becomes
    // (or replaces) the right child. Same route already sitting in the target slot gets
    // reused instead of forking a duplicate node.
    fun navigate(route: String) {
        val targetIsLeft = current.left == null
        val existing = if (targetIsLeft) current.left else current.right
        val node = if (existing?.route == route) {
            existing
        } else {
            NavNode(route, parent = current).also {
                if (targetIsLeft) current.left = it else current.right = it
            }
        }
        current = node
    }

    // Plain parent-pointer walk-up. This is the same "pop the last thing" semantics as
    // CustomNavController.popBack() - just expressed as tree traversal instead of a
    // list removal, since every node only has one parent regardless of how many children
    // it has.
    fun popBack(): Boolean {
        val parent = current.parent ?: return false
        current = parent
        return true
    }

    // Walks up parents until it finds `route`, or stops at the root if never found.
    fun popBackTo(route: String) {
        var node = current
        while (node.route != route && node.parent != null) {
            node = node.parent!!
        }
        current = node
    }

    // Resets to a fresh tree with a new root - e.g. logout/login, where old history
    // shouldn't be reachable via back at all. Mutates in place (root/current stay the
    // same object identity as before this call) rather than returning a new instance -
    // a TreeNavController is meant to live in a single `remember { }` for its whole
    // lifetime, and a caller can't reassign that val from a returned instance.
    fun reset(route: String) {
        root = NavNode(route)
        current = root
    }
}