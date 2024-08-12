package ca.ubc.ece.resess.ui

import ca.ubc.ece.resess.settings.WrapperManager
import ca.ubc.ece.resess.slicer.ParameterSpec
import ca.ubc.ece.resess.slicer.TypeOfParameter
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class SelectParametersActionGroup : ActionGroup() {

    companion object {
        val childrenMap: HashMap<ParameterSpec, ParameterGetterActionInterface> = hashMapOf()

        fun resetChildrenMap() {
            childrenMap.clear()
        }
    }


    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val children = mutableListOf<AnAction>()
        val optionalChildren = mutableListOf<AnAction>()

        val parameterSpecs : ArrayList<ParameterSpec> = WrapperManager.getCurrentWrapperMetadata().specs ?: return children.apply{add(Separator())}.toTypedArray()
        if (parameterSpecs.size == 0) {
            children.add(Separator())
            return children.toTypedArray()
        }
//        children.add(Separator())

        for (spec in parameterSpecs) {
            if (childrenMap.keys.contains(spec)) {
                val action = childrenMap[spec]!!
                if (spec.isOptional()) {
                    optionalChildren.add(action as AnAction)
                } else {
                    children.add(action as AnAction)
                }
                continue
            }
            val action = WrapperManagerUI.getExtraParameterAction(spec)
            childrenMap[spec] = action
            if (spec.isOptional()) {
                optionalChildren.add(action as AnAction)
            } else {
                children.add(action as AnAction)
            }
        }
//        children.add(Separator())
        children.addAll(optionalChildren) //necessary children first, then optional
        children.add(Separator())
        return children.toTypedArray()
    }
}

class DisplayParametersActionGroup : ActionGroup() {
    private var duplicateChildrenMap: HashMap<ParameterSpec, ParameterGetterActionInterface> = SelectParametersActionGroup.childrenMap


    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val children = mutableListOf<AnAction>()
        val optionalChildren = mutableListOf<AnAction>()

        val parameterSpecs : ArrayList<ParameterSpec> = WrapperManager.getCurrentWrapperMetadata().specs ?: return children.apply{add(Separator())}.toTypedArray()
        if (parameterSpecs.size == 0) {
            children.add(Separator())
            return children.toTypedArray()
        }

        children.add(Separator())

        var count = 0
        duplicateChildrenMap.values.forEach() {
            if(it.getStatus()) count += 1
        }

        var necessary = parameterSpecs.size
        parameterSpecs.forEach() {
            if (it.isOptional()) {
                necessary -= 1
                count -=1
            }
        }

        children.add(Separator("Necessary Extra Parameters: ${if (count <= 0) "0" else count}/${necessary}"))
        for (spec in parameterSpecs) {
            val specAction : ParameterGetterActionInterface? = if (duplicateChildrenMap.keys.contains(spec)) duplicateChildrenMap[spec] else null
            val type: String = if (spec.type == TypeOfParameter.STATEMENT) "Statement" else "Variable"
            val actionName = "[${type}] ${spec.label}${if (!spec.isOptional()) " - ${specAction?.getValues()?.size ?: "0"}/${spec.numberOfValues}" else ""}"
            val action = object: ActionGroup(actionName ,true) {
                override fun update(e: AnActionEvent) {
                    super.update(e)
                    if (specAction?.getStatus()?: false) {
                        e.presentation.icon = AllIcons.Debugger.ThreadStates.Idle
                    } else {
                        e.presentation.icon = AllIcons.General.ShowInfos
                    }
                }

                override fun getChildren(e: AnActionEvent?): Array<AnAction> {
                    val innerChildren: MutableList<AnAction> = mutableListOf()
                    if (specAction == null) {
                        return innerChildren.toTypedArray()
                    }
                    if (spec.isOptional() && specAction.getValues().isEmpty()) {
                        innerChildren.add(object : AnAction(" "){
                            override fun actionPerformed(e: AnActionEvent) {
                            }
                            override fun update(e: AnActionEvent) {
                                e.presentation.isEnabled = false
                            }
                        })
                    }
                    for (value in specAction.getValues()) {
                        val action = object : AnAction(value.toString()) { //Submenu with list of value locations
                            override fun actionPerformed(e: AnActionEvent) {
                                specAction.removeValue(value)
                            }
                        }
                        innerChildren.add(action)
                    }
                    return innerChildren.toTypedArray()
                }
            }
            if (spec.isOptional()) {
                optionalChildren.add(action)
            } else {
                children.add(action)
            }
        }
        children.add(Separator("Optional Extra Parameters:"))
        children.addAll(optionalChildren)
        children.add(Separator())
        return children.toTypedArray()
    }
}







