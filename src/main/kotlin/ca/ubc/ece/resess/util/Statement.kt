package ca.ubc.ece.resess.util

import com.intellij.openapi.actionSystem.AnActionEvent
import java.util.*

class Statement @JvmOverloads constructor(
    val clazz: String,
    val lineNo: Int, // 0 based
    val slicingContext: AnActionEvent? = null): ParameterType {
    override fun toString() = "$clazz:$lineNo"

    override fun equals(other: Any?) = (other is Statement)
            && clazz == other.clazz
            && lineNo == other.lineNo

    override fun hashCode() = Objects.hash(clazz, lineNo)
}
