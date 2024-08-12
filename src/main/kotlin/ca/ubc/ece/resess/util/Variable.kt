package ca.ubc.ece.resess.util

import ca.ubc.ece.resess.settings.WrapperManager
import ca.ubc.ece.resess.ui.SelectSlicingCriterionAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import java.util.*

class Variable(val statement: Statement,
               val name: String,
               val isDeclared: Boolean): ParameterType {
    override fun toString() = "$statement:$name"

    override fun equals(other: Any?) = (other is Variable)
            && statement.equals(other.statement)
            && name == other.name

    override fun hashCode() = Objects.hash(statement, name)

    companion object {

        fun getSliceVariables(project : Project) {
            WrapperManager.sliceVariables = ArrayList()
            assert(SelectSlicingCriterionAction.slicingCriterionStatus && WrapperManager.extraParametersStatus && SelectSlicingCriterionAction.slicingCriterion != null && SelectSlicingCriterionAction.slicingCriterion!!.slicingContext != null) { return }

            //visualize in all open editors
            val sliceLines: HashMap<String, ArrayList<Int>> = getSliceLines()
            for (fileEditor in FileEditorManager.getInstance(project).allEditors) {
                if (fileEditor is TextEditor)
                    getEditorVariables(project, fileEditor.editor, sliceLines)
            }

            val messageBusConnection = project.messageBus.connect()
            messageBusConnection
                .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                    override fun fileOpenedSync(
                        source: FileEditorManager, file: VirtualFile,
                        editorsWithProviders: MutableList<FileEditorWithProvider>
                    ) {
                        super.fileOpenedSync(source, file, editorsWithProviders)
                        for (fileEditor in editorsWithProviders.map { x -> x.fileEditor }) {
                            if (fileEditor is TextEditor)
                                getEditorVariables(project, fileEditor.editor, sliceLines)
                        }
                    }
                })
        }

        fun getEditorVariables(project: Project, editor: Editor, sliceLines: HashMap<String, ArrayList<Int>>) {
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: throw IllegalArgumentException("No psi file found")
            val document = editor.document

            psiFile.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)
                    when (element) {
                        is PsiVariable -> addVariable(element)
                        is PsiReferenceExpression -> addReference(element)
                    }
                }

                private fun addVariable(variable: PsiVariable) {
                    if (variable.name != "System.out") {
                        val lineNumber = document.getLineNumber(variable.textRange.startOffset)
                        val className = PsiTreeUtil.getParentOfType(variable, PsiClass::class.java)?.name!!
                        if (sliceLines[className]?.contains(lineNumber)?: false) {
                            WrapperManager.sliceVariables.add(Variable(Statement(className, lineNumber), variable.name!!, true))
                        }
                    }
                }

                private fun addReference(reference: PsiReferenceExpression) {
                    if (reference.text != "System.out") {
                        val resolved = reference.resolve()
                        if (resolved is PsiVariable) {
                            val lineNumber = document.getLineNumber(reference.textRange.startOffset)
                            val className = PsiTreeUtil.getParentOfType(resolved, PsiClass::class.java)?.name!!
                            if (sliceLines[className]?.contains(lineNumber)?: false) {
                                WrapperManager.sliceVariables.add(Variable(Statement(className, lineNumber), reference.text, true))
                            }
                        }
                    }
                }
            })
        }
        private fun getSliceLines(): HashMap<String, ArrayList<Int>> {
            val classLinesMap = HashMap<String, ArrayList<Int>>()
            var currentStatement : Statement? = WrapperManager.getCurrentWrapper().getFirstInSlice()?: return classLinesMap

            while (currentStatement != null) {
                val clazz = currentStatement.clazz
                val lineNo = currentStatement.lineNo
                if (classLinesMap[clazz] == null) classLinesMap[clazz] = ArrayList<Int>()
                if (!classLinesMap[clazz]!!.contains(lineNo)) {
                    classLinesMap[clazz]!!.add(lineNo)
                }
                println("adding $clazz, $lineNo to slice lines map")
                currentStatement = WrapperManager.getCurrentWrapper().nextInSlice(currentStatement)
            }

            for (clazz in classLinesMap.keys) {
                classLinesMap[clazz] = ArrayList(classLinesMap[clazz]!!.toSet())
            }

            return classLinesMap
        }

        fun getVariablesInSingleLine(e: AnActionEvent): List<Variable> {
            val variableList = ArrayList<Variable>()

            val lineNo : Int = getLineNo(e)

            val editor = e.getData(CommonDataKeys.EDITOR) ?: return variableList
            val psiFile = PsiDocumentManager.getInstance(e.project!!).getPsiFile(editor.document) ?: return variableList

            val document = editor.document

            psiFile.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)
                    when (element) {
                        is PsiVariable -> addVariable(element)
                        is PsiReferenceExpression -> addReference(element)
                    }
                }

                private fun addVariable(variable: PsiVariable) {
                    if (variable.name != "System.out") {
                        val lineNumber = document.getLineNumber(variable.textRange.startOffset) + 1
                        val className = PsiTreeUtil.getParentOfType(variable, PsiClass::class.java)?.name!!
                        if (lineNumber == lineNo) {
                            variableList.add(Variable(Statement(className, lineNumber), variable.name!!, true))
                        }
                    }
                }

                private fun addReference(reference: PsiReferenceExpression) {
                    if (reference.text != "System.out") {
                        val resolved = reference.resolve()
                        if (resolved is PsiVariable) {
                            val lineNumber = document.getLineNumber(reference.textRange.startOffset) + 1
                            val className = PsiTreeUtil.getParentOfType(resolved, PsiClass::class.java)?.name!!
                            if (lineNumber == lineNo) {
                                variableList.add(Variable(Statement(className, lineNumber), reference.text, true))
                            }
                        }
                    }
                }
            })

            return variableList
        }
        private fun getLineNo(e : AnActionEvent) : Int {
            val editor = e.getData(CommonDataKeys.EDITOR)!!
            val offset = editor.caretModel.offset
            val document = editor.document
            return document.getLineNumber(offset) + 1
        }
    }
}