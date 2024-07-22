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
        val statements : ArrayList<Statement> = getUniqueStatements(currentSlice)

        if (statements.indexOf(currentStatement) == statements.size - 1){
            return null // end of slice
        }
        for (statement in statements){
            if (statement == currentStatement){
                val index = statements.indexOf(statement)
                if (index < statements.size - 1){
                    return statements[index + 1]
                }
            }
        }
        throw IllegalArgumentException("statement not in slice")
    }
    override fun prevInSlice(currentStatement: Statement): Statement?{
        val currentSlice: Slice = getSlice()
        val statements: ArrayList<Statement> = getUniqueStatements(currentSlice)

        if (statements.indexOf(currentStatement) == 0){
            return null // start of slice
        }

        for (statement in statements){
            if (statement == currentStatement){
                val index = statements.indexOf(statement)
                if (index > 0){
                    return statements[index - 1]
                }
            }
        }
        throw IllegalArgumentException("statement not in slice")

    }

    override fun getFirstInSlice(): Statement? {
        val currentSlice: Slice = getSlice()
        if (currentSlice.statements.isEmpty()){
            return null
        }
        return currentSlice.statements[0]
    }

    private fun getUniqueStatements(slice: Slice): ArrayList<Statement> {
        val statements: ArrayList<Statement> = ArrayList()
        slice.statements.forEach {
            if (!statements.contains(it)) {
                statements.add(it)
            }
        }
        return statements
    }

}