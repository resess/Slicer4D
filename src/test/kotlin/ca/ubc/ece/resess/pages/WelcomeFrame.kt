package ca.ubc.ece.resess.pages
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.steps.Step
import com.intellij.remoterobot.steps.StepParameter
import java.time.Duration

fun RemoteRobot.welcomeFrame(function: WelcomeFrame.()-> Unit) {
    find(WelcomeFrame::class.java, Duration.ofSeconds(10)).apply(function)
}

@FixtureName("Welcome Frame")
@DefaultXpath("type", "//div[@class='FlatWelcomeFrame']")
class WelcomeFrame(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : CommonContainerFixture(remoteRobot, remoteComponent) {
    val createNewProjectLink
        get() = actionLink(byXpath("New Project","//div[(@class='MainButton' and @text='New Project') or (@accessiblename='New Project' and @class='JButton')]"))
    val moreActions
        get() = button(byXpath("More Action", "//div[@accessiblename='More Actions']"))

    val heavyWeightPopup
        get() = remoteRobot.find(ComponentFixture::class.java, byXpath("//div[@class='HeavyWeightWindow']"))

    @Step("Open project", "Open project '{1}'")
    fun openProject(@StepParameter("Project absolute path", "") absolutePath: String) {
        remoteRobot.runJs(
            """
            importClass(com.intellij.openapi.application.ApplicationManager)
            importClass(com.intellij.ide.impl.OpenProjectTask)
           
            const projectManager = com.intellij.openapi.project.ex.ProjectManagerEx.getInstanceEx()
            let task 
            try { 
                task = OpenProjectTask.build()
            } catch(e) {
                task = OpenProjectTask.newProject()
            }
            const path = new java.io.File("${absolutePath.replace("\\", "/")}").toPath()
           
            const openProjectFunction = new Runnable({
                run: function() {
                    projectManager.openProject(path, task)
                }
            })
           
            ApplicationManager.getApplication().invokeLater(openProjectFunction)
        """
        )
    }
}