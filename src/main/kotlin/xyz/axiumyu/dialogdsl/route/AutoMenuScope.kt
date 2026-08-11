package xyz.axiumyu.dialogdsl.route

import net.kyori.adventure.text.Component
import xyz.axiumyu.d
import xyz.axiumyu.dialogdsl.dialog.dsl.PaperDialogDsl

@PaperDialogDsl
class AutoMenuScope {
    val menuItems = mutableListOf<Pair<Component, RouteBase>>()

    infix fun Component.to(target: RouteBase) {
        menuItems.add(Pair(this, target))
    }

    infix fun String.to(target: RouteBase) {
        menuItems.add(Pair(d(this), target))
    }
}