package ca.ubc.ece.resess.slicer

data class ParameterSpec(
    var label: String? = null, // text to display on extension point
    var type: TypeOfParameter? = null, // type of parameter: VARIABLE or STATEMENT
    var description: String? = null, //developer describes how the user should use/select the parameter -- used in instructions
    var number: Int = 1, // 0 means unlimited, 1+ means limited to the number
    val extensionPoint: ExtensionPoint = ExtensionPoint.EDITOR_WINDOW
)

enum class ExtensionPoint {
    EDITOR_WINDOW,
    DEBUGGER
}

enum class TypeOfParameter {
    VARIABLE,
    STATEMENT
}