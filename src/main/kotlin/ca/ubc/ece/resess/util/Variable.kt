package ca.ubc.ece.resess.util

import java.util.*

class Variable(val statement: Statement,
               val name: String,
               val isDeclared: Boolean): ParameterType {
    override fun toString() = "$statement:$name"

    override fun equals(other: Any?) = (other is Variable)
            && statement.equals(other.statement)
            && name == other.name

    override fun hashCode() = Objects.hash(statement, name)
}