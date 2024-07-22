package ca.ubc.ece.resess.ui

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import ca.ubc.ece.resess.settings.WrapperManager

class SliceMenu: DefaultActionGroup() {
    private val settings = service<WrapperManager>()

    init {
        addChildren()
    }

    private fun addChildren(){
        for (metadata in settings.slicerWrappers){
            super.addAction(WrapperManagerUI.getSelectSlicerAction(metadata))
        }

        super.addSeparator()

        super.addAction(WrapperManagerUI.getEditConfigurationAction())


    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        // TODO: Change this to be more efficient
        this.removeAll()
        addChildren()
    }
}