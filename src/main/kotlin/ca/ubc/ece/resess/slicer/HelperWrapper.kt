package ca.ubc.ece.resess.slicer

import ca.ubc.ece.resess.util.Statement

abstract class HelperWrapper: APILayer{
    abstract fun getSlice(): Slice
    override fun isInSlice(currentStatement: Statement): Boolean {
        val currentSlice: Slice = getSlice()
        for (statement in currentSlice.statements){
            if (statement == currentStatement){
                return true
            }
        }
        return false
    }
    override fun nextInSlice(currentStatement: Statement): Statement?{
        val currentSlice: Slice = getSlice()
        if (currentSlice.statements.indexOf(currentStatement) == currentSlice.statements.size - 1){
            return null
        }
        for (statement in currentSlice.statements){
            if (statement == currentStatement){
                val index = currentSlice.statements.indexOf(statement)
                if (index < currentSlice.statements.size - 1){
                    return currentSlice.statements[index + 1]
                }
            }
        }
        throw IllegalArgumentException("statement not in slice")
    }
    override fun prevInSlice(currentStatement: Statement): Statement?{
        val currentSlice: Slice = getSlice()
        if (currentSlice.statements.indexOf(currentStatement) == 0){
            return null
        }
        for (statement in currentSlice.statements){
            if (statement == currentStatement){
                val index = currentSlice.statements.indexOf(statement)
                if (index > 0){
                    return currentSlice.statements[index - 1]
                }
            }
        }
        throw IllegalArgumentException("statement not in slice")

    }

}