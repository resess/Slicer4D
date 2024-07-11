package ca.ubc.ece.resess.dbgcontroller;

import ca.ubc.ece.resess.settings.WrapperManager;
import com.intellij.debugger.engine.JavaDebugProcess;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.UIUtil;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.impl.XSourcePositionImpl;
import com.intellij.xdebugger.impl.actions.XDebuggerActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ca.ubc.ece.resess.util.Statement;
import ca.ubc.ece.resess.util.Utils;

import java.util.Objects;

public class DppJavaDebugProcess extends JavaDebugProcess {
    public final BreakPointController breakPointController;
    boolean slicing;

    protected DppJavaDebugProcess(@NotNull XDebugSession session, @NotNull DebuggerSession javaSession) {
        super(session, javaSession);
        this.slicing = true;
        this.breakPointController = new BreakPointController(getDebuggerSession().getProcess());
    }

    public static DppJavaDebugProcess create(@NotNull final XDebugSession session, @NotNull final DebuggerSession javaSession) {
        DppJavaDebugProcess res = new DppJavaDebugProcess(session, javaSession);
        javaSession.getProcess().setXDebugProcess(res);
        Statement firstLine = WrapperManager.getCurrentWrapper().getFirstInSlice(); // tbd
        if (firstLine != null) {
            res.breakPointController.addBreakpoint(firstLine);
        }
        return res;
    }

    @Override
    public void runToPosition(@NotNull XSourcePosition position, @Nullable XSuspendContext context) {
        if (context == null) {
            return;
        }
        String clazz = Utils.findClassName(getSession().getProject(), position.getFile(), position.getOffset());
        Statement statement = new Statement(Objects.requireNonNull(clazz), position.getLine());
        if (!WrapperManager.getCurrentWrapper().isInSlice(statement)) {
            super.runToPosition(XSourcePositionImpl.create(position.getFile(), position.getLine()), context);
        } else {
            Messages.showErrorDialog("The line you selected is out of the slice, please try again!",
                    UIUtil.removeMnemonic(ActionsBundle.actionText(XDebuggerActions.RUN_TO_CURSOR)));
            getSession().positionReached(context);
        }
    }
}

