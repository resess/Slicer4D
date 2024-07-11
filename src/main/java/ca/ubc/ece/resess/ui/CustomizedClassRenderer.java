//package ca.ubc.ece.resess.ui;
//
//import com.intellij.debugger.DebuggerContext;
//import com.intellij.debugger.JavaDebuggerBundle;
//import com.intellij.debugger.engine.DebuggerManagerThreadImpl;
//import com.intellij.debugger.engine.DebuggerUtils;
//import com.intellij.debugger.engine.evaluation.EvaluateException;
//import com.intellij.debugger.engine.evaluation.EvaluationContext;
//import com.intellij.debugger.engine.jdi.StackFrameProxy;
//import com.intellij.debugger.impl.DebuggerUtilsAsync;
//import com.intellij.debugger.impl.DebuggerUtilsEx;
//import com.intellij.debugger.impl.DebuggerUtilsImpl;
//import com.intellij.debugger.ui.impl.watch.FieldDescriptorImpl;
//import com.intellij.debugger.ui.impl.watch.MessageDescriptor;
//import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl;
//import com.intellij.debugger.ui.tree.*;
//import com.intellij.debugger.ui.tree.render.ClassRenderer;
//import com.intellij.openapi.diagnostic.Logger;
//import com.intellij.openapi.util.*;
//import com.intellij.openapi.util.text.StringUtil;
//import com.intellij.psi.CommonClassNames;
//import com.intellij.psi.JavaPsiFacade;
//import com.intellij.psi.PsiElement;
//import com.intellij.psi.PsiElementFactory;
//import com.intellij.util.IncorrectOperationException;
//import com.intellij.util.containers.ContainerUtil;
//import com.intellij.xdebugger.frame.XCompositeNode;
//import com.intellij.xdebugger.impl.ui.XDebuggerUIConstants;
//import com.jetbrains.jdi.StringReferenceImpl;
//import com.sun.jdi.*;
//import one.util.streamex.StreamEx;
//import org.jdom.Element;
//import org.jetbrains.annotations.NonNls;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.ArrayList;
//
//public class CustomizedClassRenderer extends ClassRenderer {
//    @Override
//    protected boolean shouldDisplay(EvaluationContext context, @NotNull ObjectReference objInstance, @NotNull Field field){
//        boolean result = true;
//        ArrayList<String> variables
//
//        return (result && super.shouldDisplay(context, objInstance, field));
//    }
//}