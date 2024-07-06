package ca.ubc.ece.resess.util

import java.util.*

class Variable(val statement: Statement, val variable: String): ParameterType {
    override fun toString() = "$statement:$variable"

    override fun equals(other: Any?) = (other is Variable)
            && statement.equals(other.statement)
            && variable == other.variable

    override fun hashCode() = Objects.hash(statement, variable)
}