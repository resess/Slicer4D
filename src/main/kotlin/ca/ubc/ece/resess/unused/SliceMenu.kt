package ca.ubc.ece.resess.unused
//
//import com.intellij.openapi.actionSystem.AnActionEvent
//import com.intellij.openapi.actionSystem.DefaultActionGroup
//import com.intellij.openapi.components.service
//import ca.ubc.ece.resess.settings.WrapperManager
//import com.intellij.openapi.actionSystem.ActionGroup
//import com.intellij.openapi.actionSystem.AnAction
//import com.intellij.openapi.application.ApplicationManager
//
//class SliceMenu: ActionGroup() {
//
//
//    private fun addChildren(){
//        for (metadata in WrapperManager.slicerWrappers) {
//            super.addAction(WrapperManagerUI.getSelectSlicerAction(metadata))
//        }
//        super.addSeparator()
//        super.addAction(WrapperManagerUI.getEditConfigurationAction())
//
//    }
//
//    override fun update(e: AnActionEvent) {
//        super.update(e)
//    }
//
//    override fun getChildren(p0: AnActionEvent?): Array<AnAction> {
//        TODO("Not yet implemented")
//    }
//}