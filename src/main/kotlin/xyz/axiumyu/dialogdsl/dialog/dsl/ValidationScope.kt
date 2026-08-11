package xyz.axiumyu.dialogdsl.dialog.dsl

class DialogValidationException(val errorMsg: String) : Exception(errorMsg) {
    override fun fillInStackTrace(): Throwable = this
}

@PaperDialogDsl
@SuppressWarnings("UnstableApiUsage")
class ValidationScope {

    fun <T> T?.withError(errorMsg: String): T {
        return this ?: throw DialogValidationException(errorMsg)
    }

    fun <T> withError(errorMsg: String, block: ValidationScope.() -> T?): T {
        return block() ?: throw DialogValidationException(errorMsg)
    }

    fun withError(errorMsg: String, cond: Boolean){
        if (!cond) throw DialogValidationException(errorMsg)
    }

    fun Boolean.withError(errorMsg: String){
        if (!this) throw DialogValidationException(errorMsg)
    }
}

// 使用这个方法来开启值校验，在内部使用 withError 方法来指定错误消息
inline fun validate(
    onFailure: (String) -> Unit,
    block: ValidationScope.() -> Unit
) {
    try {
        // 实例化作用域并执行闭包
        ValidationScope().block()
    } catch (e: DialogValidationException) {
        onFailure(e.errorMsg)
    } catch (e: Exception) {
        // 兜底：如果开发者忘了用 withError 包裹，且抛出了原生异常
        onFailure(e.message ?: "发生参数错误")
    }
}