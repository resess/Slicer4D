package ca.ubc.ece.resess.unused

//import ca.ubc.ece.resess.util.Statement
//import ca.ubc.ece.resess.util.Variable
//import com.intellij.openapi.actionSystem.AnAction
//import com.intellij.openapi.actionSystem.AnActionEvent
//import com.intellij.openapi.actionSystem.CommonDataKeys
//import com.intellij.openapi.ui.Messages
//import com.intellij.psi.*
//import com.intellij.psi.util.PsiTreeUtil
//
//class TestVarGetterAction : AnAction() {
//    override fun actionPerformed(e: AnActionEvent) {
//
//        val variables = collectVariables(e)
//
//        val result = variables.joinToString("\n") { variable ->
//            "${variable.name} (${if (variable.isDeclared) "declared" else "used"}) on line ${variable.statement.lineNo + 1}"
//        }
//
//        Messages.showMessageDialog(
//            e.project,
//            result,
//            "Variables and Usages",
//            Messages.getInformationIcon()
//        )
//    }
//
//    private fun collectVariables(e: AnActionEvent): List<Variable> {
//        val variableList = mutableListOf<Variable>()
//
//        val editor = e.getData(CommonDataKeys.EDITOR) ?: return variableList
//        val psiFile = PsiDocumentManager.getInstance(e.project!!).getPsiFile(editor.document) ?: return variableList
//
//        val document = editor.document
//
//        psiFile.accept(object : PsiRecursiveElementVisitor() {
//            override fun visitElement(element: PsiElement) {
//                super.visitElement(element)
//                when (element) {
//                    is PsiVariable -> addVariable(element)
//                    is PsiReferenceExpression -> addReference(element)
//                }
//            }
//
//            private fun addVariable(variable: PsiVariable) {
//                if (variable.name != "System.out") {
//                    val lineNumber = document.getLineNumber(variable.textRange.startOffset)
//                    val className = PsiTreeUtil.getParentOfType(variable, PsiClass::class.java)?.name!!
//                    variableList.add(Variable(Statement(className, lineNumber), variable.name!!, true))
//                }
//            }
//
//            private fun addReference(reference: PsiReferenceExpression) {
//                if (reference.text != "System.out") {
//                    val resolved = reference.resolve()
//                    if (resolved is PsiVariable) {
//                        val lineNumber = document.getLineNumber(reference.textRange.startOffset)
//                        val className = PsiTreeUtil.getParentOfType(resolved, PsiClass::class.java)?.name!!
//                        variableList.add(Variable(Statement(className, lineNumber), reference.text, true))
//                    }
//                }
//            }
//        })
//
//        return variableList
//    }
//
//    override fun update(e: AnActionEvent) {
//        e.presentation.isEnabled = true
//    }
//}
//
//
//
//
//
//
////
////class TestVarGetterAction : AnAction() {
////    override fun actionPerformed(e: AnActionEvent) {
////        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
////        val psiFile = PsiDocumentManager.getInstance(e.project!!).getPsiFile(editor.document) ?: return
////
////        val document = editor.document
////
////        val variableUsages = mutableMapOf<Int, MutableList<String>>()
////
////        psiFile.accept(object : PsiRecursiveElementVisitor() {
////            override fun visitElement(element: PsiElement) {
////                super.visitElement(element)
////                when (element) {
////                    is PsiVariable -> addVariable(element)
////                    is PsiReferenceExpression -> addReference(element)
////                }
////            }
////
////            private fun addVariable(variable: PsiVariable) {
////                val lineNumber = document.getLineNumber(variable.textRange.startOffset)
////                variableUsages.computeIfAbsent(lineNumber) { mutableListOf() }.add(variable.name!!)
////            }
////
////            private fun addReference(reference: PsiReferenceExpression) {
////                val resolved = reference.resolve()
////                if (resolved is PsiVariable) {
////                    val lineNumber = document.getLineNumber(reference.textRange.startOffset)
////                    variableUsages.computeIfAbsent(lineNumber) { mutableListOf() }.add("${reference.text} (usage)")
////                }
////            }
////        })
////
////        val result = variableUsages.entries.sortedBy { it.key }
////            .joinToString("\n") { (line, variables) ->
////                "Line ${line + 1}: ${variables.joinToString(", ")}"
////            }
////
////        Messages.showMessageDialog(
////            e.project,
////            result,
////            "Variables and Usages with Line Numbers",
////            Messages.getInformationIcon()
////        )
////    }
////
////    override fun update(e: AnActionEvent) {
////        e.presentation.isEnabled = true
////    }
////}
