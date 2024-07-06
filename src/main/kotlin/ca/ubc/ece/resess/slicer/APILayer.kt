package ca.ubc.ece.resess.slicer

import ca.ubc.ece.resess.util.ParameterType
import ca.ubc.ece.resess.util.Statement
import ca.ubc.ece.resess.util.Variable

interface APILayer {
    fun getConfiguration(): ArrayList<ParameterSpec>
    fun setSlicingCriterion(statement: Statement, variables: ArrayList<Variable>? = null): Boolean
    fun setParameters(values: Map<ParameterSpec, ArrayList<ParameterType>>): Boolean
    fun isInSlice(currentStatement: Statement): Boolean
    fun nextInSlice(currentStatement: Statement): Statement?
    fun prevInSlice(currentStatement: Statement): Statement?
}