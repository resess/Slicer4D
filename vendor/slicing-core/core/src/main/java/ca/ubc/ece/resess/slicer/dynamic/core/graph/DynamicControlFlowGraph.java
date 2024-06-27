package ca.ubc.ece.resess.slicer.dynamic.core.graph;

import java.util.Set;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.util.Chain;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementInstance;
import ca.ubc.ece.resess.slicer.dynamic.core.utils.AnalysisLogger;
import ca.ubc.ece.resess.slicer.dynamic.core.utils.Constants;
import soot.toolkits.scalar.Pair;


public class DynamicControlFlowGraph extends Graph{
    private Map<SootClass, SootMethod> callbackMethods = new HashMap<>();
    private Map<Pair<SootMethod, Unit>, String> threadCallers = new HashMap<>();
    private Set<String> threadMethods = new HashSet<>();
    private Map<Pair<SootMethod, Unit>, SootClass> setterCallbackMap = new HashMap<>();
    private Map<Pair<SootMethod, Unit>, Integer> setterLineMap = new HashMap<>();
    private Map<Pair<SootMethod, Unit>, Integer> threadLineMap = new HashMap<>();
    private Set<Pair<Integer, Integer>> connectedThreads = new HashSet<>();

    private Map <Integer, StatementInstance> possibleCallbacks = new LinkedHashMap<>();
    private Map<Integer, Integer> nextUnitWithStaticField = new HashMap<>();
    private Map<Integer, Integer> previousUnitWithStaticField = new HashMap<>();
    private Map <Integer, StatementInstance> mapNumberUnits = new LinkedHashMap<>();
    private List<StatementInstance> traceList = new ArrayList<>();
    private long lastLine;
    private int prevLine = -1;

    public List<StatementInstance> getTraceList() {
        return traceList;
    }

    public void setCallbackMethods(Map<SootClass, SootMethod> callbackMethods) {
        this.callbackMethods = callbackMethods;
    }

    public void setThreadCallers(Map<Pair<SootMethod, Unit>, String> threadCallers) {
        this.threadCallers = threadCallers;
    }

    public void setThreadMethods(Set<String> threadMethods) {
        this.threadMethods = threadMethods;
    }

    public void setSetterCallbackMap(Map<Pair<SootMethod, Unit>, SootClass> setterCallbackMap) {
        this.setterCallbackMap = setterCallbackMap;
    }

    public Map<Pair<SootMethod, Unit>, SootClass> getSetterCallbackMap() {
        return setterCallbackMap;
    }


    public DynamicControlFlowGraph createDCFG(List<TraceStatement> tr) {
        Chain<SootClass> chain = Scene.v().getApplicationClasses();
        Map<String, SootMethod> allMethods = createMethodsMap(chain);
        Map<SootMethod, Map <String, List<Unit>>> unitStringsCache = new HashMap<>();

        AnalysisLogger.log(true, "Trace size: {}", tr.size());
        StatementInstance previousStatement = null;
        StatementInstance createdStatement = null;
        SootMethod oldMethod = null;
        long timeStamp = System.currentTimeMillis();
        int lineNumber = -1;
        for (TraceStatement traceStatement : tr) {
            lineNumber = lineNumber + 1;
            if (lineNumber%100000==0) {
                long newTimeStamp = System.currentTimeMillis();
                AnalysisLogger.log(true, "Progress: {}/{} ({}), time: {}", lineNumber, tr.size(),  lineNumber*100/tr.size(),(newTimeStamp - timeStamp)/1e6);
                timeStamp = newTimeStamp;
            }
            String methodName = traceStatement.getMethod();
            SootMethod mt = allMethods.get(methodName);
            try {
                if(mt.getActiveBody()==null) { 
                    continue;
                }
            } catch(Exception ex) {
                AnalysisLogger.warn(true, "Checking stmt. {}, whith method name {}, Exception: {}", traceStatement, methodName, ex);
                continue;
            }
            if (mt.getDeclaringClass().getName().startsWith(Constants.ANDROID_LIBS)) {
                continue;
            }
            boolean firstInMethod = false;
            if (!mt.equals(oldMethod)) {
                firstInMethod = true;
            }
            oldMethod = mt;
            Body body = mt.getActiveBody();

            Map<String, Pair<SootMethod, Unit>> settersInThisMethod = new HashMap<>();
            Map<String, Pair<SootMethod, Unit>> threadStartersInThisMethod = new HashMap<>();
            PatchingChain<Unit> units = body.getUnits();

            Map <String, List<Unit>> unitString;
            if (unitStringsCache.containsKey(mt)) {
                unitString = unitStringsCache.get(mt);
            } else {
                unitString = createUnitStrings(units);
                unitStringsCache.put(mt, unitString);
                updateSettersMaps(mt, units, settersInThisMethod, threadStartersInThisMethod);
            }

            createdStatement = createStatementInstance(mt, unitString, settersInThisMethod, threadStartersInThisMethod, traceStatement, lineNumber);
            if (createdStatement != null) {
                addControlFlows(previousStatement, createdStatement);
                previousStatement = createdStatement;
                setLastLine(createdStatement.getLineNo());
                if (firstInMethod) {
                    addRegisterationEdgesForCallbacks(mt, lineNumber);
                    addRegisterationEdgesForThreads(mt, lineNumber);
                }
            }
        }
        cleanGraphFromFalseEdges(connectedThreads);
        fixThreadsGraph();
        removeReturnEdges();
        savePossibleCallback();
        saveStaticFields();
        addInFrameworkCalls();
        return this;
    }

    public void addInFrameworkCalls() {
        // add println to toString calls
        Deque<StatementInstance> prevUnits = new ArrayDeque<>();
        for (int i = 0; i < getLastLine(); i++) {
            StatementInstance iu = mapNoUnits(i);
            if (iu == null) {
                continue;
            }
            for (StatementInstance prevUnit : prevUnits) {
                SootMethod prevCalled = prevUnit.getCalledMethod();
                if (prevCalled != null && prevCalled.getSignature().equals("<java.io.PrintStream: void println(java.lang.Object)>")) {
                    if (iu.getMethod().getSubSignature().equals("java.lang.String toString()")) {
                        if (predecessorListOf(iu.getLineNo()).isEmpty() && !iu.getMethod().getDeclaringClass().getName().startsWith(Constants.ANDROID_LIBS)) {
                            setEdgeType(prevUnit.getLineNo(), iu.getLineNo(), EdgeType.CALL_EDGE);
                            break;
                        }
                    }
                }
                if (prevCalled != null && prevCalled.getSignature().equals("<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.Object)>")) {
                    if (iu.getMethod().getSubSignature().equals("java.lang.String toString()")) {
                        if (predecessorListOf(iu.getLineNo()).isEmpty() && !iu.getMethod().getDeclaringClass().getName().startsWith(Constants.ANDROID_LIBS)) {
                            setEdgeType(prevUnit.getLineNo(), iu.getLineNo(), EdgeType.CALL_EDGE);
                            break;
                        }
                    }
                }
            }

            if (prevUnits.size() > 100) {
                prevUnits.removeLast();
            }
            prevUnits.push(iu);
        }
    }

    private void addStatement(StatementInstance statementInstance) {
        mapNumberUnits.put(statementInstance.getLineNo(), statementInstance);
        traceList.add(statementInstance);
    }

    private StatementInstance createStatementInstance(SootMethod mt, Map<String, List<Unit>> unitString,
            Map<String, Pair<SootMethod, Unit>> settersInThisMethod,
            Map<String, Pair<SootMethod, Unit>> threadStartersInThisMethod, TraceStatement traceStatement, int lineNumber) {
        StatementInstance createdStatement = null;
        if(unitString.containsKey(traceStatement.getInstruction())) {
            createdStatement = matchStatementInstanceToTraceLine(mt, unitString, settersInThisMethod, threadStartersInThisMethod, traceStatement, lineNumber);
        }

        if (createdStatement == null) {
            createdStatement = matchStatementInstanceToClosestTraceLine(mt, unitString, traceStatement, lineNumber);
        }
        return createdStatement;
    }

    private StatementInstance matchStatementInstanceToClosestTraceLine(SootMethod mt, Map<String, List<Unit>> unitString, 
                    TraceStatement traceStatement, int lineNumber) {

        int leastDistance = Integer.MAX_VALUE;
        String second = traceStatement.getInstruction();
        Unit closestUnit = null;
        for(String us: unitString.keySet()) {
            String first = us;
            if (first.contains("if") && first.contains("goto")) {
                first = first.substring(0, first.indexOf("goto"));
            }
            if (second.contains("if") && second.contains("goto")) {
                second = second.substring(0, second.indexOf("goto"));
            }
            if (StringUtils.getCommonPrefix(first, second).length() > 0) {
                int threshold = Math.min(first.length(), second.length())/2;
                int distance = (new LevenshteinDistance(threshold)).apply(first, second);
                if (distance == -1) {
                    distance = threshold;
                }
                if (distance < leastDistance) {
                    List<Unit> units = unitString.get(us);
                    if (units.size() == 1) {
                        closestUnit = units.get(0);
                    } else {
                        int idx = getClosestUnitByLineNumber(units, prevLine);
                        closestUnit = units.get(idx);
                    }
                    leastDistance = distance;
                }
            }
        }
        StatementInstance createdStatement = null;
        if (closestUnit != null) {
            createdStatement = new StatementInstance(mt, closestUnit, lineNumber, traceStatement.getThreadId(), traceStatement.getFieldAddr(), closestUnit.getJavaSourceStartLineNumber(), mt.getDeclaringClass().getFilePath());
            addStatement(createdStatement);
        } else {
            AnalysisLogger.warn(true, "Cannot create instruction {}", traceStatement);
        }
        return createdStatement;
    }

    private StatementInstance matchStatementInstanceToTraceLine(SootMethod mt, Map<String, List<Unit>> unitString,
            Map<String, Pair<SootMethod, Unit>> settersInThisMethod,
            Map<String, Pair<SootMethod, Unit>> threadStartersInThisMethod, TraceStatement traceStatement, int lineNumber) {
        StatementInstance createdStatement = null;
        String us = traceStatement.getInstruction();
        List<Unit> units = unitString.get(us);
        try {
            Unit unit = null;
            if (units.size() == 1) {
                unit = units.get(0);
            } else {
                int idx = getClosestUnitByLineNumber(units, prevLine);
                unit = units.get(idx);
            }
            createdStatement = new StatementInstance(mt, unit, lineNumber, traceStatement.getThreadId(), traceStatement.getFieldAddr(), unit.getJavaSourceStartLineNumber(), mt.getDeclaringClass().getFilePath());
            prevLine = createdStatement.getJavaSourceLineNo();
            addStatement(createdStatement);
            if (settersInThisMethod.containsKey(us)) {
                setterLineMap.put(settersInThisMethod.get(us), lineNumber);
            }
            if (threadStartersInThisMethod.containsKey(us)) {
                threadLineMap.put(threadStartersInThisMethod.get(us), lineNumber);
            }
        } catch (Exception e) {
            AnalysisLogger.error("Cannot create instruction {}", traceStatement);
        }
        return createdStatement;
    }

    public static int getClosestUnitByLineNumber(List<Unit> units, int prevLine) {
        int distance = Math.abs(units.get(0).getJavaSourceStartLineNumber() - prevLine);
        int idx = 0;
        for(int c = 1; c < units.size(); c++){
            int cdistance = Math.abs(units.get(c).getJavaSourceStartLineNumber() - prevLine);
            if(cdistance < distance){
                idx = c;
                distance = cdistance;
            }
        }
        return idx;
    }

    private void addRegisterationEdgesForThreads(SootMethod mt, int lineNumber) {
        for (String threadMethod: threadMethods) {
            if (threadMethod.equals(mt.getSignature())) {
                for (Pair<SootMethod, Unit> threadStarters : threadCallers.keySet()) {
                    int source = threadLineMap.get(threadStarters);
                    setEdgeType(source, lineNumber, EdgeType.CALL_EDGE);
                }
            }
        }
    }

    private void addRegisterationEdgesForCallbacks(SootMethod mt, int lineNumber) {
        for (SootMethod cb: callbackMethods.values()) {
            if (cb.equals(mt)) {
                for (Pair<SootMethod, Unit> callbackSetter : setterCallbackMap.keySet()) {
                    connectCallback(mt, lineNumber, callbackSetter);
                }
            }
        }
    }

    private void connectCallback(SootMethod mt, int lineNumber, Pair<SootMethod, Unit> callbackSetter) {
        try {
            if (callbackMethods.get(setterCallbackMap.get(callbackSetter)).equals(mt)) {
                int source = setterLineMap.get(callbackSetter);
                setEdgeType(source, lineNumber, EdgeType.CALL_EDGE);
            }
        } catch (Exception e) {
            // pass
        }
    }


    protected static Map<String, List<Unit>> createUnitStrings(PatchingChain<Unit> units) {
        Map<String, List<Unit>> unitString = new LinkedHashMap<>();
        for(Unit u: units) {
            if (u instanceof IdentityStmt) {
                continue;
            }
            List<Unit> val = unitString.getOrDefault(u.toString(), new ArrayList<>());
            val.add(u);
            unitString.put(u.toString(), val);
        }
        return unitString;
    }

    private void updateSettersMaps(SootMethod mt, PatchingChain<Unit> units,
            Map<String, Pair<SootMethod, Unit>> settersInThisMethod,
            Map<String, Pair<SootMethod, Unit>> threadStartersInThisMethod) {
        for(Unit u: units) {
            if (u instanceof IdentityStmt) {
                continue;
            }
            Pair<SootMethod, Unit> methodUnitPair = new Pair<>(mt, u);
            if (setterCallbackMap.containsKey(methodUnitPair)) {
                settersInThisMethod.put(u.toString(), methodUnitPair);
            }
            if (threadCallers.containsKey(methodUnitPair)) {
                threadStartersInThisMethod.put(u.toString(), methodUnitPair);
            }
        }
    }

    protected static Map<String, SootMethod> createMethodsMap(Chain<SootClass> chain) {
        Map<String, SootMethod> allMethods = new HashMap<>();
        Iterator<SootClass> iterator = chain.snapshotIterator();
        while(iterator.hasNext()) {
            SootClass sc = iterator.next();
            List<SootMethod> methods = sc.getMethods();
            for(SootMethod mt:methods) {
                allMethods.put(mt.getSignature(), mt);
            }
        }
        return allMethods;
    }

    private void addControlFlows(StatementInstance previous, StatementInstance current) {
        if (previous != null && current != null) {
            if (previous.getMethod().equals(current.getMethod()) && !current.getMethod().equals(previous.getCalledMethod())) {
                setEdgeType(previous.getLineNo(), current.getLineNo(), EdgeType.FLOW_EDGE);
            }
        }
    }

    private void savePossibleCallback(){
        for (int i = 0; i < getLastLine(); i++) {
            StatementInstance iu = mapNoUnits(i);
            if (iu == null) {
                continue;
            }
            if (predecessorListOf(iu.getLineNo()).isEmpty() && !iu.getMethod().getDeclaringClass().getName().startsWith(Constants.ANDROID_LIBS)) {
                addToPossibleCalbacks(iu.getLineNo(), iu);
            }
        }
    }

    private void saveStaticFields(){
        int prevUnit = -1;
        for (int i = 0; i < getLastLine(); i++) {
            StatementInstance iu = mapNoUnits(i);
            if (iu == null) {
                continue;
            }
            Unit u = iu.getUnit();
            if (u instanceof AssignStmt) {
                AssignStmt stmt = (AssignStmt) u;
                Value left = stmt.getLeftOp();
                Value right = stmt.getRightOp();
                if (left instanceof FieldRef) {
                    if (((FieldRef) left).getUseBoxes().isEmpty()) {
                        putStaticFieldUnitEdge(prevUnit, iu.getLineNo());
                        prevUnit = iu.getLineNo();
                    }
                } else if (right instanceof FieldRef) {
                    if (((FieldRef) right).getUseBoxes().isEmpty()) {
                        putStaticFieldUnitEdge(prevUnit, iu.getLineNo());
                        prevUnit = iu.getLineNo();
                    }
                }
            }
        }
    }

    private void cleanGraphFromFalseEdges(Set<Pair<Integer, Integer>> connectedThreads){
        Set<Edge> removed = new HashSet<>();
        for (Map.Entry<Integer, List<Edge>> entry: getEdgeSet()) {
            for (Edge e: entry.getValue()) {
                int source = e.getSource();
                int target = e.getDestination();
                if (e.getEdgeType().equals(EdgeType.CALL_EDGE) || e.getEdgeType().equals(EdgeType.RETURN_EDGE)) {
                    if (connectedThreads.contains(new Pair<>(source, target)) || mapNoUnits(source) == null || mapNoUnits(target) == null){
                        continue;
                    }
                    removeFlowEdgesAccrossThreads(removed, e, source, target);
                } else if (e.getEdgeType().equals(EdgeType.FLOW_EDGE)) {
                    removeFlowEdgesAfterReturn(removed, e, source);
                }
            }
        }
        removeAllEdges(removed);
    }

    private void removeFlowEdgesAfterReturn(Set<Edge> removed, Edge e, int source) {
        if (mapNoUnits(source) != null && mapNoUnits(source).getUnit() != null && !mapNoUnits(source).getUnit().fallsThrough()) {
            // Removing false flow edges after returns 
            removed.add(e);
        }
    }

    private void removeFlowEdgesAccrossThreads(Set<Edge> removed, Edge e, int source, int target) {
        if (!mapNoUnits(source).getThreadID().equals( 
            mapNoUnits(target).getThreadID())) {
                removed.add(e);
        }
    }

    private void removeReturnEdges(){
        Set<Edge> removed = new HashSet<>();
        for (Map.Entry<Integer, List<Edge>> entry: getEdgeSet()){
            for (Edge e: entry.getValue()) {
                int source = e.getSource();
                if (e.getEdgeType().equals(EdgeType.FLOW_EDGE)) {
                    // Removing false flow edges after returns 
                    if (mapNoUnits(source) != null && mapNoUnits(source).getUnit() != null && mapNoUnits(source).isReturn()) {
                        removed.add(e);
                    }
                }
            }
        }
        removeAllEdges(removed);
    }

    private void fixThreadsGraph(){
        Map<Long, ArrayList<StatementInstance>> threadTraces = separateThreads();
        for (Long tid: threadTraces.keySet()) {
            ArrayList<StatementInstance> unitsInThread = threadTraces.get(tid);
            for (int i = unitsInThread.size()-1; i >=0; i--) {
                StatementInstance iu = unitsInThread.get(i);
                if (iu == null) {
                    continue;
                }
                fixConnectToCallers(unitsInThread, i);
            }
        }
    }

    private void fixFlowEdges(ArrayList<StatementInstance> unitsInThread, int i) {
        boolean fixed = false;
        if (predecessorListOf(unitsInThread.get(i).getLineNo()).isEmpty()) {
            for (int j = i - 1; j >= 0; j--) {
                Unit possibleCaller = unitsInThread.get(j).getUnit();
                if (possibleCaller == null) {
                    continue;
                }
                fixed = connectFlowEdgesInSameMethod(unitsInThread, i, fixed, j, possibleCaller);
                if (fixed) {
                    break;
                }
            }
        }
        if (predecessorListOf(unitsInThread.get(i).getLineNo()).isEmpty()) {
            conntectToPrev(unitsInThread, i);
        }
    }

    private boolean connectFlowEdgesInSameMethod(ArrayList<StatementInstance> unitsInThread, int i, boolean fixed,
            int j, Unit possibleCaller) {
        if (unitsInThread.get(j).getMethod().equals(unitsInThread.get(i).getMethod()) && isReturn(possibleCaller)){
            fixed = true;
        }
        if (unitsInThread.get(j).getMethod().equals(unitsInThread.get(i).getMethod()) && !isReturn(possibleCaller)){
            Set<Edge> removed = new HashSet<>();
            removeNonCallerEdge(unitsInThread.get(i).getLineNo(), removed);
            removeAllEdges(removed);
            setEdgeType(unitsInThread.get(j).getLineNo(), unitsInThread.get(i).getLineNo(), EdgeType.FLOW_EDGE);
            fixed = true;
        }
        return fixed;
    }

    private void fixConnectToCallers(ArrayList<StatementInstance> unitsInThread, int i) {
        if (predecessorListOf(unitsInThread.get(i).getLineNo()).isEmpty()) {
            connectToCaller(unitsInThread, i);
        } else if (predecessorListOf(unitsInThread.get(i).getLineNo()).size() == 1) {
            int posCaller = predecessorListOf(unitsInThread.get(i).getLineNo()).get(0);
            if (!unitsInThread.get(i).getMethod().equals(mapNoUnits(posCaller).getMethod()) ) {
                connectToCaller(unitsInThread, i);
            }
        }
    }

    private Map<Long, ArrayList<StatementInstance>> separateThreads() {
        Map<Long, ArrayList<StatementInstance>> threadTraces = new HashMap<>();
        for (int i = 0; i < getLastLine(); i++) {
            StatementInstance iu = mapNoUnits(i);
            if (iu == null) {
                continue;
            }
            Long tid = iu.getThreadID();
            if (tid != null) {
                ArrayList<StatementInstance> l = threadTraces.get(tid);
                if (l == null) {
                    l = new ArrayList<>();
                }
                l.add(iu);
                threadTraces.put(tid, l);
            }
        }
        return threadTraces;
    }

    private boolean calledMethodInAppClasses(Unit uu) {
        return Scene.v().getApplicationClasses().contains(((Stmt) uu).getInvokeExpr().getMethod().getDeclaringClass());
    }

    private boolean methodIsInAndroidLibs(ArrayList<StatementInstance> unitsInThread, int i) {
        return !mapNoUnits(unitsInThread.get(i).getLineNo()-1).getMethod().getDeclaringClass().getName().startsWith(Constants.ANDROID_LIBS);
    }

    private boolean conntectToPrev(ArrayList<StatementInstance> unitsInThread, int i) {
        Set<Edge> removed = new HashSet<>();
        boolean connected = false;
        if (mapNoUnits(unitsInThread.get(i).getLineNo()-1) == null){
            return connected;
        }
        Unit possibleCaller = mapNoUnits(unitsInThread.get(i).getLineNo()-1).getUnit();
        if (((Stmt) possibleCaller).containsInvokeExpr()) {
            if (((Stmt) possibleCaller).getInvokeExpr().getMethod().getSubSignature().equals(unitsInThread.get(i).getMethod().getSubSignature())) {
                connected = addCallEdgeToPrev(unitsInThread, i, removed, connected, possibleCaller);
            } else if (unitsInThread.get(i).getMethod().getName().equals("<clinit>")) {
                connected = addCallEdgeFromClassConstructorToPrev(unitsInThread, i, removed, connected, possibleCaller);
            }
        }
        removeAllEdges(removed);
        return connected;
    }

    private boolean addCallEdgeFromClassConstructorToPrev(ArrayList<StatementInstance> unitsInThread, int i,
            Set<Edge> removed, boolean connected, Unit possibleCaller) {
        if (((Stmt) possibleCaller).getInvokeExpr().getMethod().getDeclaringClass().getName().equals(unitsInThread.get(i).getMethod().getDeclaringClass().getName())) {
            Edge e = getEdge(unitsInThread.get(i).getLineNo()-1, unitsInThread.get(i).getLineNo());
            if (e == null){
                removePrevEdges(unitsInThread, i, removed);
                setEdgeType(unitsInThread.get(i).getLineNo()-1, unitsInThread.get(i).getLineNo(), EdgeType.CALL_EDGE);
                connected = true;
            }
        }
        return connected;
    }

    private boolean addCallEdgeToPrev(ArrayList<StatementInstance> unitsInThread, int i,
            Set<Edge> removed, boolean connected, Unit possibleCaller) {
        if (calledMethodInAppClasses(possibleCaller) && methodIsInAndroidLibs(unitsInThread, i)) {
            Edge e = getEdge(unitsInThread.get(i-1).getLineNo(), unitsInThread.get(i).getLineNo());
            if (e == null){
                removePrevEdges(unitsInThread, i, removed);
                setEdgeType(unitsInThread.get(i).getLineNo()-1, unitsInThread.get(i).getLineNo(), EdgeType.CALL_EDGE);
                connected = true;
            }
        }
        return connected;
    }

    private void removePrevEdges(ArrayList<StatementInstance> unitsInThread, int i, Set<Edge> removed) {
        for (int pred: predecessorListOf(unitsInThread.get(i).getLineNo())){
            Edge p = getEdge(pred, unitsInThread.get(i).getLineNo());
            if (p != null){
                removed.add(p);
            }
        }
    }

    private void connectToCaller(ArrayList<StatementInstance> unitsInThread, int i) {
        StatementInstance iIu = unitsInThread.get(i);
        SootMethod iIuMethod = iIu.getMethod();
        int iIuLineNo = iIu.getLineNo();
        Set<Edge> removed = new HashSet<>();
        int stop = i-Constants.SEARCH_LENGTH;
        if (stop < 0) {
            stop = 0;
        }

        for (int j = i-1; j >= stop; j--) {
            StatementInstance jIu = unitsInThread.get(j);
            if (jIu == null) {
                return;
            }

            int jIuLineNo = jIu.getLineNo();
            SootMethod jIuMethod = jIu.getMethod();

            boolean flowEdgeAdded = addFlowEdgeWithinMethod(iIuMethod, iIuLineNo, removed, jIuLineNo, jIuMethod);
            if (flowEdgeAdded) {
                break;
            }

            
            boolean callEdgeAdded = addCallEdgeBetweenMethods(iIuMethod, iIuLineNo, removed, jIuLineNo, jIuMethod, jIu);
            if (callEdgeAdded) {
                break;
            }
        }
        removeAllEdges(removed);
    }

    private boolean addCallEdgeBetweenMethods(SootMethod iIuMethod, int iIuLineNo, Set<Edge> removed,
            int jIuLineNo, SootMethod jIuMethod, StatementInstance jIu) {
        boolean callEdgeAdded = false;
        SootMethod calledMethod = jIu.getCalledMethod();
        if (calledMethod != null && classInAndroidLibs(jIuMethod)) {
            if (calledMethod.getSubSignature().equals(iIuMethod.getSubSignature())) {
                callEdgeAdded = addCallEdgeToRegularCaller(iIuLineNo, removed, jIuLineNo, callEdgeAdded, iIuMethod);
            } else if (iIuMethod.getName().equals("<clinit>") && classNamesMatch(iIuMethod, calledMethod)) {
                callEdgeAdded = addCallEdgeToClassConstructor(iIuLineNo, removed, jIuLineNo, callEdgeAdded);
            }
        }
        return callEdgeAdded;
    }

    private boolean addCallEdgeToClassConstructor(int iIuLineNo, Set<Edge> removed, int jIuLineNo,
            boolean callEdgeAdded) {
        Edge e = getEdge(jIuLineNo, iIuLineNo);
        if (e == null){
            removeNonCallerEdge(iIuLineNo, removed);
            setEdgeType(jIuLineNo, iIuLineNo, EdgeType.CALL_EDGE);
            callEdgeAdded = true;
        }
        return callEdgeAdded;
    }

    private boolean addCallEdgeToRegularCaller(int iIuLineNo, Set<Edge> removed, int jIuLineNo,
            boolean callEdgeAdded, SootMethod iIuMethod) {
        if (Scene.v().getApplicationClasses().contains(iIuMethod.getDeclaringClass())) {
            callEdgeAdded = addCallEdgeToClassConstructor(iIuLineNo, removed, jIuLineNo, callEdgeAdded);
        }
        return callEdgeAdded;
    }

    private boolean addFlowEdgeWithinMethod(SootMethod iIuMethod, int iIuLineNo, Set<Edge> removed,
            int jIuLineNo, SootMethod jIuMethod) {
        boolean flowEdgeAdded = false;
        if (jIuMethod.equals(iIuMethod)) {
            Edge e = getEdge(jIuLineNo, iIuLineNo);
            if (e == null){
                boolean isReturn = isReturn(mapNoUnits(jIuLineNo).getUnit());
                boolean alreadyHasFlowEdge = successorListOf(jIuLineNo).stream().anyMatch(s -> getEdge(jIuLineNo, s).getEdgeType().equals(EdgeType.FLOW_EDGE));
                if (!isReturn && !alreadyHasFlowEdge) {
                    removeNonCallerEdge(iIuLineNo, removed);
                    setEdgeType(jIuLineNo, iIuLineNo, EdgeType.FLOW_EDGE);
                    flowEdgeAdded = true;
                }
            } else {
                flowEdgeAdded = true;
            }
        }
        return flowEdgeAdded;
    }

    private boolean classNamesMatch(SootMethod iIuMethod, SootMethod calledMethod) {
        return calledMethod.getDeclaringClass().getName().equals(iIuMethod.getDeclaringClass().getName());
    }

    private boolean classInAndroidLibs(SootMethod jIuMethod) {
        return !jIuMethod.getDeclaringClass().getName().startsWith(Constants.ANDROID_LIBS);
    }

    private void removeNonCallerEdge(int iIuLineNo, Set<Edge> removed) {
        for (int pred: predecessorListOf(iIuLineNo)){
            Edge p = getEdge(pred, iIuLineNo);
            if (p != null){
                removed.add(p);
            }
        }
    }

    boolean isReturn(Unit u){
        return (u instanceof ReturnStmt) || (u instanceof ReturnVoidStmt);
    }


    public StatementInstance mapNoUnits(int num){
        return mapNumberUnits.get(num);
    }

    public void setLastLine(long lastLine) {
        this.lastLine = lastLine;
    }

    public long getLastLine() {
        return lastLine;
    }

    public List<StatementInstance> mapNoUnits(List<Integer> num){
        List<StatementInstance> l = new ArrayList<>();
        for (int n: num) {
            l.add(mapNoUnits(n));
        }
        return l;
    }

    public void addToPossibleCalbacks(int location, StatementInstance iu){
        this.possibleCallbacks.put(location, iu);
    }

    public void putStaticFieldUnitEdge(int v1, int v2) {
        this.nextUnitWithStaticField.put(v1, v2);
        this.previousUnitWithStaticField.put(v2, v1);
    }

    public int getNextUnitWithStaticField(int source) {
        return nextUnitWithStaticField.get(source);
    }

    public int getPreviousUnitWithStaticField(int source) {
        return previousUnitWithStaticField.get(source);
    }

    public Map<Integer, StatementInstance> getMapNumberUnits() {
      return mapNumberUnits;
    }
}

