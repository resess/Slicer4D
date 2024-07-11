package ca.ubc.ece.resess.ui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import ca.ubc.ece.resess.settings.WrapperManager
import ca.ubc.ece.resess.settings.WrapperManagerUI

class SliceMenu: DefaultActionGroup() {
    private val settings = service<WrapperManager>()

    init {
        addChildren()
    }

    private fun addChildren(){
        //TODO: fix list
        for (pair in settings.slicerWrappers){
            super.addAction(WrapperManagerUI.getSelectSlicerAction(pair.first, pair.second))
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