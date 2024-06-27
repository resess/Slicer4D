package ca.ubc.ece.resess.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.ui.Gray
import ca.ubc.ece.resess.slicer.ProgramSlice


class EditorSliceVisualizer(private val project: Project, private val slice: ProgramSlice) {
    companion object {
        private val LOG = Logger.getInstance(EditorSliceVisualizer::class.java)
        private val greyOutAttributes = TextAttributes()
        val GREY_OUT_COLOR = Gray._77

        init {
            greyOutAttributes.foregroundColor = GREY_OUT_COLOR
        }

        /**
         * Called in [ca.ubc.ece.resess.pages.EditorComponentImplFixture.getGreyedOutLines]
         */
        @JvmStatic
        fun getGreyedOutLines(editor: Editor): HashSet<Int> {
            val greyedOutLines = HashSet<Int>()
            for (highlighter in editor.markupModel.allHighlighters) {
                if (highlighter.targetArea == HighlighterTargetArea.LINES_IN_RANGE) {
                    if (GREY_OUT_COLOR == highlighter.getTextAttributes(null)?.foregroundColor) {
                        greyedOutLines.add(editor.document.getLineNumber(highlighter.startOffset))
                        greyedOutLines.add(editor.document.getLineNumber(highlighter.endOffset))
                    }
                }
            }
            return greyedOutLines
        }
    }

    private val messageBusConnection = project.messageBus.connect()
    private val psiManager = PsiManager.getInstance(project)

    fun start() {
        LOG.info("Start")
        // First remove all previous greyouts
        ApplicationManager.getApplication().invokeAndWait { removeAllGreyOuts() }
        // Add new greyouts
        visualizeInExistingEditors()
        messageBusConnection
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun fileOpenedSync(
                    source: FileEditorManager, file: VirtualFile,
                    editorsWithProviders: MutableList<FileEditorWithProvider>
                ) {
                    super.fileOpenedSync(source, file, editorsWithProviders)
                    for (fileEditor in editorsWithProviders.map { x -> x.fileEditor }) {
                        if (fileEditor is TextEditor)
                            visualizeInEditor(fileEditor)
                    }
                }
            })
    }

    fun stop() {
        LOG.info("Stop")
        messageBusConnection.disconnect()
        ApplicationManager.getApplication().invokeAndWait { removeAllGreyOuts() }
    }

    private fun visualizeInExistingEditors() {
        for (fileEditor in FileEditorManager.getInstance(project).allEditors) {
            if (fileEditor is TextEditor)
                visualizeInEditor(fileEditor)
        }
    }

    private fun removeAllGreyOuts() {
        for (fileEditor in FileEditorManager.getInstance(project).allEditors) {
            if (fileEditor is TextEditor) {
                val toRemove = ArrayList<RangeHighlighter>()
                for (highlighter in fileEditor.editor.markupModel.allHighlighters) {
                    if (highlighter.getTextAttributes(null) == greyOutAttributes) {
                        toRemove.add(highlighter)
                    }
                }
                for (highlighter in toRemove) {
                    fileEditor.editor.markupModel.removeHighlighter(highlighter)
                }
            }
        }
    }

    private fun visualizeInEditor(textEditor: TextEditor) {
        LOG.info("visualizeInEditor $textEditor")
        val file = psiManager.findFile(textEditor.file)
        if (file !is PsiJavaFile)
            return

        val fileIndex = ProjectRootManager.getInstance(file.project).fileIndex
        if (!fileIndex.isInContent(file.virtualFile)) {
            return
        }

        val sliceLines = HashSet<Int>()
        for (clazz in file.classes) {
            slice.sliceLinesUnordered[clazz.qualifiedName]?.let { lines ->
                sliceLines.addAll(lines)
            }
        }

        for (line in 0 until textEditor.editor.document.lineCount) {
            if (sliceLines.contains(line))
                continue
            textEditor.editor.markupModel.addLineHighlighter(
                line,
                HighlighterLayer.SELECTION + 1,
                greyOutAttributes
            )
        }
    }
}
