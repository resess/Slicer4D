package ca.ubc.ece.resess.dbgcontroller.pages;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.*;
import com.intellij.remoterobot.fixtures.ContainerFixture;
import com.intellij.remoterobot.fixtures.JButtonFixture;
import com.intellij.remoterobot.search.locators.Locator;
import com.intellij.remoterobot.utils.Keyboard;
import org.assertj.swing.core.MouseButton;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.Assertions;
import org.junit.runners.MethodSorters;
import org.testng.annotations.Test;
import ca.ubc.ece.resess.pages.EditorComponentImplFixture;
import ca.ubc.ece.resess.pages.IdeaFrame;
import ca.ubc.ece.resess.pages.WelcomeFrame;

import java.io.File;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UITest {
    private static RemoteRobot robot;
    private static final Locator DEBUGGER_PP_UP_BTN = byXpath("//div[@class='ActionButton' and @myaction='Debug with Dynamic Slicing using Debugger++ (Debug selected configuration with dynamic slicing using Debugger++)']");
    private static final Locator DEBUGGER_PP_DOWN_BTN = byXpath("//div[@text='Debugger++']");


    // Utils
    private void clearAndWrite(Keyboard keyboard, String text, int textLength) {
        for(int i = 0; i < textLength; i++) {
            keyboard.backspace();
        }
        keyboard.enterText(text);
    }

    private void clearAndWrite(Keyboard keyboard, String text) {
        clearAndWrite(keyboard, text, 10);
    }

    public static void openProject() throws InterruptedException {
        System.out.println("Set up");

        if (!robot.getFinder().findMany(byXpath("//div[@class='FlatWelcomeFrame']")).isEmpty()) {
            System.out.println("Enter Welcome page");
            WelcomeFrame welcomeFrame = robot.find(WelcomeFrame.class);
            welcomeFrame.openProject(new File("src/test/TestProject").getAbsolutePath());
            System.out.println("Finished Welcome page\n");
        }

        // Open project file
        final IdeaFrame idea = robot.find(IdeaFrame.class, ofSeconds(10));
        waitFor(ofMinutes(5), () -> !idea.isDumbMode());
        if (robot.getFinder().findMany(byXpath("//div[@class='ProjectViewTree']")).isEmpty()) {
            robot.find(JButtonFixture.class, byXpath("//div[@tooltiptext='Project']"), Duration.ofSeconds(10)).clickWhenEnabled();
        }

        step("Open Main.java", () -> {
            final ContainerFixture projectView = idea.getProjectViewTree();
            if (!projectView.hasText("src")) {
                projectView.findText(idea.getProjectName()).doubleClick();
                waitFor(() -> projectView.hasText("src"));
                projectView.findText("src").doubleClick();
            }
            if (!projectView.hasText("Main")) {
                projectView.findText(idea.getProjectName()).doubleClick();
                waitFor(() -> projectView.hasText("src"));
                projectView.findText("src").doubleClick();
            }
            projectView.findText("Main").doubleClick();
        });

        // Wait for indexes to load
        waitFor(ofMinutes(5), () -> !idea.isDumbMode());
        if (!robot.getFinder().findMany(byXpath("//div[@text='Got It']")).isEmpty()) {
            robot.find(JButtonFixture.class, byXpath("//div[@text='Got It']")).click();
        }
        System.out.println("Finished Indexing\n");
        Assertions.assertFalse(robot.getFinder().findMany(DEBUGGER_PP_UP_BTN).isEmpty());
    }

    @Test
    public void uiTestMain() throws InterruptedException {
        String javaVersion = System.getProperty("java.version");
        assertTrue(javaVersion.startsWith("11"));
        robot = new RemoteRobot("http://127.0.0.1:8082");

        openProject();
        testDebuggerppBtnWithoutSlicingCriteria();
        testDebuggerppBtnNothingMsg();
        testRightClick();
        checkGreyedOutLines();
        testDebuggerppActions();
        testDebuggerppElement();
        testDebuggerSkipNonSliceLine();
        useOriginalDebugger();
    }


    private void testDebuggerppBtnWithoutSlicingCriteria() throws InterruptedException {
        System.out.println("Click on Debugger Button");
        JButtonFixture debuggerppUpBtn = robot.find(JButtonFixture.class, DEBUGGER_PP_UP_BTN);
        debuggerppUpBtn.clickWhenEnabled();
        waitFor(ofMinutes(1), () -> !robot.getFinder().findMany(byXpath("//div[@class='MyDialog']")).isEmpty());
        Thread.sleep(1000);
        Assertions.assertFalse(robot.getFinder().findMany(byXpath("//div[@class='MyDialog']")).isEmpty());
        Assertions.assertTrue(robot.find(JButtonFixture.class, byXpath("//div[@text.key='button.ok']")).isEnabled());
        System.out.println("No Slicing Criteria Windows popped up");

        // Debugger++ down button should appear
        Assertions.assertFalse(robot.getFinder().findMany(DEBUGGER_PP_DOWN_BTN).isEmpty());
        Assertions.assertTrue(robot.find(JButtonFixture.class, DEBUGGER_PP_DOWN_BTN).isEnabled());
        System.out.println("Debugger++ down button appeared");
        robot.find(JButtonFixture.class, byXpath("//div[@text.key='button.ok']")).click();
    }
    private void testDebuggerppBtnNothingMsg(){
        if(!robot.getFinder().findMany(DEBUGGER_PP_DOWN_BTN).isEmpty()){
            robot.find(JButtonFixture.class, DEBUGGER_PP_DOWN_BTN).click();
            //display "nothing to show"
            Assertions.assertFalse(robot.getFinder().findMany(byXpath("//div[@visible_text='Nothing to show']")).isEmpty());
            robot.find(JButtonFixture.class, byXpath("//div[@myvisibleactions='[Show Options Menu (null), Hide (Hide active tool window)]']//div[@myaction.key='tool.window.hide.action.name']")).click();
        }
        else{
            System.out.println("Cannot find expect element\n");
        }
    }

    private void testRightClick() throws InterruptedException {
        if(!robot.getFinder().findMany(byXpath("//div[@class='EditorComponentImpl']")).isEmpty()){
//            int offset = robot.find(EditorFixture.class, byXpath("//div[@class='EditorComponentImpl']")).getCaretOffset();
            robot.find(EditorFixture.class, byXpath("//div[@class='EditorComponentImpl']")).clickOnOffset(609, MouseButton.RIGHT_BUTTON, 1);
        }
        else{
            System.out.println("Cannot find expect element\n");
        }
        robot.find(JButtonFixture.class, byXpath("//div[@text='Start Slicing from Line']"), Duration.ofSeconds(10)).click();
        Thread.sleep(5000);

        //if there is a Decompiler Warning
        if (!robot.getFinder().findMany(byXpath("//div[@class='DialogRootPane']")).isEmpty()) {
            System.out.println("Warning window popped up\n");
            robot.find(JButtonFixture.class, byXpath("//div[contains(@text.key, 'button.accept')]"), Duration.ofSeconds(10)).click();
        }
    }

    private void checkGreyedOutLines() throws InterruptedException {
        Thread.sleep(5000);
        EditorComponentImplFixture idea = robot.find(EditorComponentImplFixture.class, ofSeconds(10));
        HashSet<Integer> greyedOutLines = idea.getGreyedOutLines();
        assertEquals(greyedOutLines, Set.of(1, 2, 4, 5, 6, 7, 8, 10, 15, 18, 19, 20, 21, 22, 24));
    }

    private void testDebuggerppActions() throws InterruptedException {
        Thread.sleep(3000);
        //step into
        Thread.sleep(1000);
        robot.find(JButtonFixture.class, byXpath("//div[@tooltiptext.key='action.StepInto.text']"), Duration.ofSeconds(10)).click();
        //step over
        Thread.sleep(1000);
        robot.find(JButtonFixture.class, byXpath("//div[@tooltiptext.key='action.StepOver.text']"), Duration.ofSeconds(10)).click();
        //run to cursor
        Thread.sleep(1000);
        robot.find(EditorFixture.class, byXpath("//div[@accessiblename.key='editor.for.file.accessible.name']"), Duration.ofSeconds(10)).clickOnOffset(609, MouseButton.LEFT_BUTTON, 1);
        robot.find(JButtonFixture.class, byXpath("//div[@tooltiptext.key='action.RunToCursor.text']"), Duration.ofSeconds(10)).click();

        exitWindow();
    }

    private void testDebuggerppElement() throws InterruptedException {
        testRightClick();
        robot.find(JButtonFixture.class, byXpath("//div[@accessiblename='Debugger++' and @class='SimpleColoredComponent']"), Duration.ofSeconds(10)).click();
        Assertions.assertTrue(robot.find(JLabelFixture.class, byXpath("//div[@text='Data Dep']"), Duration.ofSeconds(10)).isVisible());
        Assertions.assertTrue(robot.find(JLabelFixture.class, byXpath("//div[@text='Control Dep']"), Duration.ofSeconds(10)).isVisible());
        Assertions.assertTrue(robot.find(JLabelFixture.class, byXpath("//div[@text='Graph']"), Duration.ofSeconds(10)).isVisible());
        //step into twice
        Thread.sleep(500);
        robot.find(JButtonFixture.class, byXpath("//div[@tooltiptext.key='action.StepInto.text']"), Duration.ofSeconds(10)).click();
        Thread.sleep(500);
        robot.find(JButtonFixture.class, byXpath("//div[@tooltiptext.key='action.StepInto.text']"), Duration.ofSeconds(10)).click();
        Assertions.assertTrue(
                robot.find(
                        JButtonFixture.class,
                        byXpath("//div[@accessiblename='To Line 15 (Main.java):\u00A0\u00A0 int r = z + 5;' and @class='JButton']"),
                        Duration.ofSeconds(10)).isEnabled()
        );

        //switch to Control Dep
        robot.find(JLabelFixture.class, byXpath("//div[@text='Control Dep']"), Duration.ofSeconds(10)).click();
        Assertions.assertTrue(
                robot.find(
                        JButtonFixture.class,
                        byXpath("//div[@accessiblename='To Line 15 (Main.java):\u00A0\u00A0 int r = z + 5;' and @class='JButton']"),
                        Duration.ofSeconds(10)).isEnabled()
        );
        //switch to Graph
        robot.find(JLabelFixture.class, byXpath("//div[@text='Graph']"), Duration.ofSeconds(10)).click();
        Assertions.assertTrue(
                robot.find(
                        JButtonFixture.class,
                        byXpath("//div[@class='JViewport']//div[@class='JLabel']"),
                        Duration.ofSeconds(10)).isEnabled()
        );

        //switch to Control Dep
        robot.find(JLabelFixture.class, byXpath("//div[@text='Control Dep']"), Duration.ofSeconds(10)).click();
        //click on the button to jump

        exitWindow();
    }

    private void testDebuggerSkipNonSliceLine() throws InterruptedException {
        testRightClick();
        //step into
        Thread.sleep(15000);
        waitFor(Duration.ofSeconds(10), ()-> !robot.getFinder().findMany(byXpath("//div[@class='XDebuggerFramesList'][@visible_text='main:10, Main']")).isEmpty());

        robot.find(JButtonFixture.class, byXpath("//div[@tooltiptext.key='action.StepInto.text']"), Duration.ofSeconds(10)).click();

//        waitFor(ofMinutes(1), () -> !robot.getFinder().findMany(byXpath("//div[@class='XDebuggerFramesList']")).isEmpty());
//        List<RemoteComponent> a = robot.getFinder().findMany(byXpath("//div[@class='XDebuggerFramesList'][@visible_text='main:10, Main']"));

        // Check stack frame
        waitFor(Duration.ofSeconds(10), ()-> !robot.getFinder().findMany(byXpath("//div[@class='XDebuggerFramesList'][@visible_text='test:14, Main || main:10, Main']")).isEmpty());
        //step over twice
        Thread.sleep(500);
        robot.find(JButtonFixture.class, byXpath("//div[@tooltiptext.key='action.StepOver.text']"), Duration.ofSeconds(10)).click();
        Thread.sleep(500);
        robot.find(JButtonFixture.class, byXpath("//div[@tooltiptext.key='action.StepOver.text']"), Duration.ofSeconds(10)).click();
        //check if the non-sliced line has been skipped

        exitWindow();
    }
    private void useDebuggerpp() throws InterruptedException {
        testRightClick();
//        robot.find(XDebuggerFramesList.class, byXpath("//div[@class='XDebuggerFramesList']"), Duration.ofSeconds(10));

        //div[@accessiblename='Debugger++' and @class='SimpleColoredComponent']

    }

    private void useOriginalDebugger() throws InterruptedException {
        robot.find(JButtonFixture.class, byXpath("//div[@myicon='startDebugger.svg']"), Duration.ofSeconds(10)).click();
        Thread.sleep(5000);
        //step into
        Thread.sleep(500);
        robot.find(JButtonFixture.class, byXpath("//div[@tooltiptext.key='action.StepInto.text']"), Duration.ofSeconds(10)).click();

        //step over
        Thread.sleep(500);
        robot.find(JButtonFixture.class, byXpath("//div[@tooltiptext.key='action.StepOver.text']"), Duration.ofSeconds(10)).click();
        Thread.sleep(500);
        robot.find(JButtonFixture.class, byXpath("//div[@tooltiptext.key='action.StepOver.text']"), Duration.ofSeconds(10)).click();

        //run to cursor
        Thread.sleep(1000);
        robot.find(EditorFixture.class, byXpath("//div[@accessiblename.key='editor.for.file.accessible.name']"), Duration.ofSeconds(10)).clickOnOffset(609, MouseButton.LEFT_BUTTON, 1);
        robot.find(JButtonFixture.class, byXpath("//div[@tooltiptext.key='action.RunToCursor.text']"), Duration.ofSeconds(10)).click();

        exitWindow();
    }

    private void exitWindow(){
        //exit original debugger mode
        robot.find(JButtonFixture.class, byXpath("//div[contains(@myvisibleactions, 'VCS')]//div[contains(@myaction.key, 'action.stop')]"), Duration.ofSeconds(10)).click();
        //hide window
        robot.find(JButtonFixture.class, byXpath("//div[@myvisibleactions='[Show Options Menu (null), Hide (Hide active tool window)]']//div[@myaction.key='tool.window.hide.action.name']"), Duration.ofSeconds(10)).click();
    }
}
