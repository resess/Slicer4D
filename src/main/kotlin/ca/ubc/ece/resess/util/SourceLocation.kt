package ca.ubc.ece.resess.util

import java.util.*

class SourceLocation(val clazz: String, val lineNo: Int) {
    override fun toString() = "$clazz:$lineNo"

    override fun equals(other: Any?) = (other is SourceLocation)
            && clazz == other.clazz
            && lineNo == other.lineNo

    override fun hashCode() = Objects.hash(clazz, lineNo)
}
