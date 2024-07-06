package ca.ubc.ece.resess.ui.dependencies

import ca.ubc.ece.resess.slicer.ControlDependency
import ca.ubc.ece.resess.slicer.DataDependency
import ca.ubc.ece.resess.slicer.Dependency
import ca.ubc.ece.resess.slicer.ProgramSlice
import ca.ubc.ece.resess.util.Statement
import ca.ubc.ece.resess.util.Utils
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.StatusText
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel


abstract class DependenciesPanel(protected val project: Project) : JPanel() {
    val programSlice = ProgramSlice.getcurrentProgramSlice()
    private lateinit var root: DefaultMutableTreeNode
    private lateinit var treeModel: DefaultTreeModel
    private lateinit var tree: Tree
    companion object {
        val YELLOW: Color = Color.decode("#FFC000")
//        val GREEN: Color = Color.decode("#00B050")
    }

    init {
        preferredSize = Dimension(10000, 100)
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createEmptyBorder(0, 10, 0, 10)
    }

    protected fun addTitleLabel(location: Statement) {
        val l = JButton()

        val (displayName, lineText) = getLineButtonInfo(l, location)

        l.text = "<html>To Line ${location.lineNo} ($displayName):</font>" +
                "&nbsp&nbsp" +
                "<font color='#999999'>$lineText</font>" +
                "</html>"
        l.foreground = YELLOW
        l.isFocusPainted = false
        l.margin = JBUI.emptyInsets()
        l.isContentAreaFilled = false
        l.isBorderPainted = false
        l.isOpaque = false
        l.horizontalAlignment = SwingConstants.LEFT
        l.maximumSize = Dimension(10000, 18)

        root = DefaultMutableTreeNode(l)
        treeModel = DefaultTreeModel(root)
        tree = Tree(treeModel)
        setProperties(tree)
        val treeView = JBScrollPane(tree)
        add(treeView)
    }

    private fun setProperties(tree: Tree){
        // Add a listener to the tree node to perform the button action
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {

                val path = tree.getPathForLocation(e.x, e.y)
                if (path != null && path.pathCount >= 2) {
                    val selectedNode = path.path[path.pathCount - 1] as DefaultMutableTreeNode
                    if (selectedNode.userObject is JButton) {
                        val button = selectedNode.userObject as JButton
                        button.doClick()
                    }
                }
            }
        })

        // Set a custom cell renderer for the tree to display the buttons
        tree.cellRenderer = object : DefaultTreeCellRenderer() {
            override fun getTreeCellRendererComponent(
                tree: JTree?,
                value: Any?,
                sel: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean
            ): Component {
                val component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
                if (value is DefaultMutableTreeNode && value.userObject is JButton) {
                    return value.userObject as JButton
                }
                return component
            }
            override fun getPreferredSize(): Dimension {
                val preferredSize = super.getPreferredSize()
                // Set a larger width to accommodate the full text
                preferredSize.width = 10000
                return preferredSize
            }
        }

        // Add a tree expansion listener to dynamically fetch and add child nodes when expanding
        tree.addTreeWillExpandListener(object : TreeWillExpandListener {
            override fun treeWillExpand(event: TreeExpansionEvent) {
                val node = event.path.lastPathComponent as? DefaultMutableTreeNode ?: return
                if (node.userObject is JButton) {
                    val parentButton = node.userObject as? JButton ?: return
                    val parentDependency = parentButton.getClientProperty("dependency") as? Dependency ?: return
                    // Remove all children nodes if there is a single child that contains an empty string
                    if (node.childCount == 1) { // Remove the dummy node
                        val child = node.getChildAt(0) as? DefaultMutableTreeNode
                        if (child?.userObject == "") {
                            node.removeAllChildren()
                        }
                    }
                    else return
                    if(parentDependency is DataDependency){
                        val dependencies: Collection<DataDependency>? =
                            programSlice!!.dependencies[parentDependency.location]?.data?.from
                        if(dependencies!=null){
                            for (childDependency in dependencies) {
                                if (childDependency.variableName.isEmpty())
                                    continue
                                val childButton = getButton("${childDependency.variableName}: ", childDependency)
                                val child = DefaultMutableTreeNode(childButton)
                                // Add a dummy child node to allow expanding the leaf node
                                child.add(DefaultMutableTreeNode(""))
                                node.add(child)
                            }
                        }
                    }
                    else if(parentDependency is ControlDependency){
                        val dependencies: Collection<ControlDependency>? =
                            programSlice!!.dependencies[parentDependency.location]?.control?.from
                        if(dependencies!=null){
                            for (childDependency in dependencies) {
                                val childButton = getButton("", childDependency)
                                val child = DefaultMutableTreeNode(childButton)
                                // Add a dummy child node to allow expanding the leaf node
                                child.add(DefaultMutableTreeNode(""))
                                node.add(child)
                            }
                        }
                    }
                }
            }

            override fun treeWillCollapse(event: TreeExpansionEvent) {
                // No action needed when the tree is collapsed
            }
        })
    }

    protected fun addEmptyLabel() {
        val l = JLabel("None")
        l.foreground = JBColor.GRAY
        l.border = BorderFactory.createEmptyBorder(0, 10, 0, 0)
        add(l)
    }

    protected fun addNoDependenciesMessage(name: String) {
        val l = JLabel("$name dependencies of this line is unavailable")
        l.border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
        add(l)
    }

    protected fun addDependencyLine(prefix: String, dependency: Dependency) {

        val l = getButton(prefix, dependency)
        val childNode = DefaultMutableTreeNode(l)
        root.add(childNode)
        // Add a dummy child node to allow expanding the leaf node
        childNode.add(DefaultMutableTreeNode(""))
    }

    private fun getButton(prefix: String, dependency: Dependency): JButton{
        val l =JButton()
        val (displayName, lineText) = getLineButtonInfo(l, dependency.location)
        l.text = "<html>${prefix}" +
                "<font color='#5693E2'>Line ${dependency.location.lineNo} ($displayName)</font>" +
                "&nbsp&nbsp" +
                "<font color='#999999'>$lineText</font>" +
                "</html>"
        l.isFocusPainted = false
        l.margin = JBUI.emptyInsets()
        l.isContentAreaFilled = false
        l.isBorderPainted = false
        l.isOpaque = false
        l.border = BorderFactory.createEmptyBorder(0, 10, 0, 0)
        l.horizontalAlignment = SwingConstants.LEFT
        l.maximumSize = Dimension(l.preferredSize.width, 18)
        l.putClientProperty("dependency", dependency)
        return l
    }

    private fun getLineButtonInfo(button: JButton, location: Statement): Array<String> {
        var displayName = ""
        var lineText = ""

        Utils.findPsiFile(location.clazz, project)?.let { file ->
            val logicalLineNo = location.lineNo - 1
            if (logicalLineNo < 0) {
                return@let
            }
            displayName = file.name
            button.addActionListener {
                OpenFileDescriptor(project, file.virtualFile, logicalLineNo, Int.MAX_VALUE)
                        .navigate(false)
            }
            val document = FileDocumentManager.getInstance().getDocument(file.virtualFile)
            if (document != null) {
                val start = document.getLineStartOffset(logicalLineNo)
                val end = document.getLineEndOffset(logicalLineNo)
                lineText = document.getText(TextRange(start, end))
            }
        }

        return arrayOf(displayName, lineText)
    }

    fun emptyPanel(text: String) {
        removeAll()
        val statusText = object : StatusText(this) {
            override fun isStatusVisible(): Boolean {
                return true
            }
        }
        statusText.text = text
        layout = GridBagLayout()
        add(statusText.component)
        updateUI()
    }
}