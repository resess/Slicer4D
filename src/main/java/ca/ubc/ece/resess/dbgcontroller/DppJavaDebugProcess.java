package ca.ubc.ece.resess.dbgcontroller;

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
import ca.ubc.ece.resess.slicer.ProgramSlice;
import ca.ubc.ece.resess.util.Statement;
import ca.ubc.ece.resess.util.Utils;

import java.util.Set;

public class DppJavaDebugProcess extends JavaDebugProcess {
    public final ProgramSlice slice;
    public final BreakPointController breakPointController;
    boolean slicing;

    protected DppJavaDebugProcess(@NotNull XDebugSession session, @NotNull DebuggerSession javaSession, ProgramSlice slice) {
        super(session, javaSession);
        this.slice = slice;
        this.slicing = true;
        this.breakPointController = new BreakPointController(getDebuggerSession().getProcess());
    }

    public static DppJavaDebugProcess create(@NotNull final XDebugSession session, @NotNull final DebuggerSession javaSession, ProgramSlice slice) {
        DppJavaDebugProcess res = new DppJavaDebugProcess(session, javaSession, slice);
        javaSession.getProcess().setXDebugProcess(res);
        Statement firstLine = slice.getFirstLine();
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
        Set<Integer> lines = slice.getSliceLinesUnordered().get(clazz);
        if (lines != null && lines.contains(position.getLine())) {
            super.runToPosition(XSourcePositionImpl.create(position.getFile(), position.getLine()), context);
        } else {
            Messages.showErrorDialog("The line you selected is out of the slice, please try again!",
                    UIUtil.removeMnemonic(ActionsBundle.actionText(XDebuggerActions.RUN_TO_CURSOR)));
            getSession().positionReached(context);
        }
    }
}

