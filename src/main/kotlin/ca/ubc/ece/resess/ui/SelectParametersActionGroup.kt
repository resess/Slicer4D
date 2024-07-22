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
        val parameterSpecs : ArrayList<ParameterSpec> = WrapperManager.getCurrentWrapperMetadata().specs ?: return children.toTypedArray()
        if (parameterSpecs.size == 0) {
            return children.toTypedArray()
        }

        for (spec in parameterSpecs) {
            if (childrenMap.keys.contains(spec)) {
                val action = childrenMap[spec]!!
//                action.templatePresentation.icon = if (action.getStatus()) AllIcons.Debugger.ThreadStates.Idle else AllIcons.Actions.InSelection
                children.add(action as AnAction)
                continue
            }


            val action = WrapperManagerUI.getExtraParameterAction(spec)
//            action.templatePresentation.icon = if (action.getStatus()) AllIcons.Debugger.ThreadStates.Idle else AllIcons.Actions.InSelection
            childrenMap[spec] = action
            children.add(action as AnAction)
        }

        return children.toTypedArray()
    }
}


class DisplayParametersActionGroup : ActionGroup() {
    private var childrenMap: HashMap<ParameterSpec, ParameterGetterActionInterface> = SelectParametersActionGroup.childrenMap


    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val children = mutableListOf<AnAction>()

        val parameterSpecs : ArrayList<ParameterSpec> = WrapperManager.getCurrentWrapperMetadata().specs ?: return children.toTypedArray()
        if (parameterSpecs.size == 0) {
            return children.toTypedArray()
        }

        var count = 0
        childrenMap.values.forEach() {
            if(it.getStatus()) count +=1
        }

        children.add(Separator("Extra Parameters so far: $count/${parameterSpecs.size}"))
        for (spec in parameterSpecs) {
            val specAction : ParameterGetterActionInterface? = if (childrenMap.keys.contains(spec)) childrenMap[spec] else null
            val type: String = if (spec.type == TypeOfParameter.STATEMENT) "Statement" else "Variable"
            val actionName = "[${type}] ${spec.label} - ${if (spec.numberOfValues == 0) "unlimited" else "${specAction?.getValues()?.size ?: "0"}/${spec.numberOfValues}"}"
            val action = object: ActionGroup(actionName ,true) {
                override fun update(e: AnActionEvent) {
                    super.update(e)
                    e.presentation.icon = AllIcons.General.ShowInfos
                }

                override fun getChildren(e: AnActionEvent?): Array<AnAction> {
                    val innerChildren: MutableList<AnAction> = mutableListOf()
                    if (specAction == null) {
                        return innerChildren.toTypedArray()
                    }
                    for (value in specAction.getValues()) {
                        val action = object : AnAction(value.toString()) {
                            override fun actionPerformed(e: AnActionEvent) {
                                //TODO (FUTURE): implement option to remove statement/var from values list if clicked
                                //values.remove(value)
                                //status = false
                                //WrapperManager.setExtraParameter(Pair(spec, values))
                            }
                        }
                        innerChildren.add(action)
                    }
                    return innerChildren.toTypedArray()
                }
            }
            children.add(action)
        }
        children.add(Separator())
        return children.toTypedArray()
    }
}







