package ca.ubc.ece.resess.slicer

import ca.ubc.ece.resess.util.Statement
import com.intellij.openapi.project.Project

data class Slice(
    val statements: ArrayList<Statement> = ArrayList(),
    val project: Project? = null
)