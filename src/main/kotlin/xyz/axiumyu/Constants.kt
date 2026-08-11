package xyz.axiumyu

import net.kyori.adventure.text.format.TextColor.color

enum class Color(rgb: Int){
    LIGHT_PINK(0xFFEDFA),
    PINK(0xE6AED6),
    DEEP_PINK(0xCC78B4),

    LIGHT_BLUE(0xA3FFFC),
    BLUE(0x5CE6E1),
    DEEP_BLUE(0x21CCC6),

    LIGHT_GREEN(0xC2FFC2),
    GREEN(0x87E687),
    DEEP_GREEN(0x56CC56),

    LIGHT_PURPLE(0xDAC9FF),
    PURPLE(0xAC93E6),
    DEEP_PURPLE(0x8564CC),

    WARN(0xF05179),
    TIPS(0xFFEA3A),
    INFO(0xCCCCCC);

    val color = color(rgb)
}
