package ca.ubc.ece.resess.slicer

import ca.ubc.ece.resess.util.ParameterType
import ca.ubc.ece.resess.util.Statement
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

class Slice(
    val statements: ArrayList<Statement> = ArrayList(),
    val project: Project? = null
) {
//    companion object {
//        private val LOG = Logger.getInstance(ProgramSlice::class.java)
//        private var currentProgramSlice: ProgramSlice? = null
//
//        fun getcurrentProgramSlice(): ProgramSlice? {
//            return currentProgramSlice
//        }
//
//        fun setcurrentProgramSlice(programSlice: ProgramSlice) {
//            currentProgramSlice = programSlice
//        }
//    }
}