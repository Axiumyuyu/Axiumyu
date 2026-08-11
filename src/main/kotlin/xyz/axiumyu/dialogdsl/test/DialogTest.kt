package xyz.axiumyu.dialogdsl.test

import xyz.axiumyu.d
import xyz.axiumyu.dialogdsl.dialog.BaseDialog
import xyz.axiumyu.dialogdsl.dialog.DialogSetup
import xyz.axiumyu.dialogdsl.dialog.build
import xyz.axiumyu.dialogdsl.dialog.dsl.UIType
import java.time.Duration

// example
val newDialog = DialogSetup {
    DialogContent(d("Title")) {
        canCloseWithEscape(true)
        NumRangeInput("test2", d("<aqua>输入数字"), 0f to 100f, 0f, 1.0f, 300)
        BoolInput("xyz/axiumyu/paperDialogDsl/test", d("勾选<sprite:blocks:block/stone>"))
        TextInput("test3", d("<sprite:\"minecraft:items\":item/porkchop>请输入文本"))
    }
    DialogType(UIType.NOTICE) {
        Button(
            d("1 right"),
            d("2 right"),
            100,
            -1,
            Duration.ofMinutes(5)
        ) { view, audience ->
            val test2 = view.getFloat("test2") ?: return@Button
            audience.sendMessage(d("test2: $test2"))
        }
    }
}

// use .build() to turn this to a dialog that can be opened.
val dialog1 = newDialog.build()

val dialog2: BaseDialog = DialogSetup {

    DialogContent(d("title2")) {

        // if uncomment these lines, you can not register it in bootstrap. See https://github.com/PaperMC/Paper/issues/13555
//        val item = ItemType.DIAMOND.createItemStack(1).apply {
//            setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
//        }
//        ItemDisplay(item) {
//            showDecorations(true)
//            description(DialogBody.plainMessage(d("123"), 20))
//        }


        Text(d("this is a test text123123123"))

        BoolInput("bool1", d("设置1"))
        BoolInput("bool2", d("设置2"))
        BoolInput("bool3", d("设置3"))
    }

    DialogType(UIType.MULTI_ACTION) {
        Button(
            d("get bool1"),
            d("hover1"),
            20
        ) { view, audience ->
            audience.sendMessage(d("bool1: ${view.getBoolean("bool1")}"))
        }
        Button(
            d("get bool2"),
            d("hover2"),
            20
        ) { view, audience ->
            audience.sendMessage(d("bool2: ${view.getBoolean("bool2")}"))
        }
        Button(
            d("get bool3"),
            d("hover3"),
            20
        ) { view, audience ->
            audience.sendMessage(d("bool1: ${view.getBoolean("bool3")}"))
        }
        Button(
            d("get bool4"),
            d("hover4"),
            20
        ) { view, audience ->
            audience.sendMessage(d("nope"))
        }
    }
}
