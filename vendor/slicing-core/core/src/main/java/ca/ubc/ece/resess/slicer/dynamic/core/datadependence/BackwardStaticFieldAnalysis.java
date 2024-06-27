package ca.ubc.ece.resess.slicer.dynamic.core.datadependence;


import ca.ubc.ece.resess.slicer.dynamic.core.accesspath.AccessPath;
import ca.ubc.ece.resess.slicer.dynamic.core.accesspath.AliasSet;
import ca.ubc.ece.resess.slicer.dynamic.core.framework.FrameworkModel;
import ca.ubc.ece.resess.slicer.dynamic.core.graph.DynamicControlFlowGraph;
import ca.ubc.ece.resess.slicer.dynamic.core.graph.Traversal;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementInstance;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementMap;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementSet;
import ca.ubc.ece.resess.slicer.dynamic.core.utils.Constants;
import ca.ubc.ece.resess.slicer.dynamic.core.utils.AnalysisCache;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

public class BackwardStaticFieldAnalysis {
    private StatementInstance startUnit;
    private AccessPath startField;
    private StatementSet aliasPath;
    private DynamicControlFlowGraph icdg;
    private Traversal traversal;
    
    public BackwardStaticFieldAnalysis(DynamicControlFlowGraph icdg, StatementInstance startUnit, AccessPath ap, StatementSet aliasPath, AnalysisCache analysisCache) {
        this.icdg = icdg;
        this.startUnit = startUnit;
        this.startField = ap;
        this.aliasPath = aliasPath;
        this.traversal = new Traversal(icdg, analysisCache);
    }

    public void run() {
        AliasSet taintSet = new AliasSet();
        boolean found = findBackwards(taintSet);
        if (!found) {
            findForward(taintSet);
        }
    }

    private boolean findForward(AliasSet taintSet) {
        boolean found = false;
        int pos = startUnit.getLineNo()+1;
        long end = (long) (pos + Constants.SEARCH_LENGTH);
        end = (end> icdg.getLastLine())? icdg.getLastLine(): end;
        while (pos < end) {
            StatementInstance si = icdg.mapNoUnits(pos);
            if (si == null || !si.getMethod().getName().equals("<clinit>")) {
                pos++;
                continue;
            }
 
            Pair<Boolean, Boolean> flags = new Pair<>();
            flags.setO1(false);
            flags.setO2(false);
            pos = matchFieldForward(pos, si, taintSet, flags);
            found = flags.getO1();
            boolean newPosFound = flags.getO2();
            if (found) {
                break;
            }
            if (!newPosFound) {
                pos++;
            }
        }
        return found;
    }

    private int matchFieldForward(int pos, StatementInstance si, AliasSet taintSet, Pair<Boolean, Boolean> flags) {
        int newPos = pos;
        Unit u = si.getUnit();
        if (u instanceof AssignStmt) {
            AssignStmt stmt = (AssignStmt) u;
            Value left = stmt.getLeftOp();
            Value right = stmt.getRightOp();
            if ((left instanceof FieldRef) && ((FieldRef) left).getUseBoxes().isEmpty()) {
                AccessPath var = new AccessPath(((FieldRef) left).getField().getDeclaringClass().getName(), ((FieldRef) left).getField().getType(), AccessPath.NOT_USED, si.getLineNo(), si);
                var.add(((FieldRef) left).getField().getName(), ((FieldRef) left).getField().getType(), si);
                var.setStaticField();
                if (startField.startsWith(var)) {
                    AccessPath rightAp = new AccessPath(right.toString(), right.getType(), si.getLineNo(), AccessPath.NOT_DEFINED, si);
                    AccessPath newAp = new AccessPath(rightAp, si).add(startField.getAfter(var).getO1(), startField.getAfter(var).getO2(), si); 
                    taintSet.add(newAp);
                    aliasPath.add(si);
                    flags.setO1(true);
                } else {
                    newPos = gotToNextStaticField(pos);
                    flags.setO2(true);
                }
            } else if (right instanceof FieldRef) {
                if (!matchReferenceVaraibleDefintion(si, startField.getField(), left, right)) {
                    newPos = gotToNextStaticField(pos);
                    flags.setO2(true);
                }
            }
        }
        return newPos;
    }

    private boolean matchReferenceVaraibleDefintion(StatementInstance possibleIu, String fieldName, Value left, Value right) {
        String usedField = ((FieldRef) right).getField().getName();
        StatementMap chunk = traversal.getForwardChunk(possibleIu.getLineNo());
        for (StatementInstance prev: chunk.values()) {
            if (prev.getLineNo() <= possibleIu.getLineNo()) {
                continue;
            }
            if (usedField.equals(fieldName)) {
                Stmt prevStmt = (Stmt) prev.getUnit();
                if (prevStmt.containsInvokeExpr() && traversal.isFrameworkMethod(prev)) {
                    InvokeExpr expr = prevStmt.getInvokeExpr();
                    if (expr instanceof InstanceInvokeExpr) {
                        AccessPath instance = new AccessPath(left.toString(), left.getType(), startUnit.getLineNo(), startUnit.getLineNo(), startUnit);
                        if (FrameworkModel.definesInstance(prev, instance)) {
                            aliasPath.add(prev);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private int gotToNextStaticField(int pos) {
        int prevPos = pos;
        try {
            pos = this.icdg.getNextUnitWithStaticField(pos);
        } catch (NullPointerException e) {
            pos = prevPos + 1;
        }
        return pos;
    }

    private int goToPrevStaticField(int pos) {
        int prevPos = pos;
        try {
            pos = this.icdg.getPreviousUnitWithStaticField(pos);
        } catch (NullPointerException e) {
            pos = prevPos - 1;
        }
        return pos;
    }

    private boolean findBackwards(AliasSet taintSet) {
        int pos = startUnit.getLineNo()-1;
        boolean found = false;

        while (pos > -1) {
            StatementInstance si = icdg.mapNoUnits(pos);
            if (si == null) {
                pos--;
                continue;
            }

            Pair<Boolean, Boolean> flags = new Pair<>();
            flags.setO1(false);
            flags.setO2(false);
            pos = matchFieldBackward(pos, si, taintSet, flags);
            found = flags.getO1();
            boolean newPosFound = flags.getO2();
            if (found) {
                break;
            }
            if (!newPosFound) {
                pos--;
            }
        }
        return found;
    }

    private int matchFieldBackward(int pos, StatementInstance si, AliasSet taintSet, Pair<Boolean, Boolean> flags) {
        int newPos = pos;
        Unit u = si.getUnit();
        if (u instanceof AssignStmt) {
            AssignStmt stmt = (AssignStmt) u;
            Value left = stmt.getLeftOp();
            Value right = stmt.getRightOp();
            if ((left instanceof FieldRef) && ((FieldRef) left).getUseBoxes().isEmpty()) {
                AccessPath var = new AccessPath(((FieldRef) left).getField().getDeclaringClass().getName(), ((FieldRef) left).getField().getType(), AccessPath.NOT_USED, si.getLineNo(), si);
                var.add(((FieldRef) left).getField().getName(), ((FieldRef) left).getField().getType(), si);
                var.setStaticField();
                if (startField.startsWith(var)) {
                    AccessPath rightAp = new AccessPath(right.toString(), right.getType(), si.getLineNo(), AccessPath.NOT_DEFINED, si);
                    AccessPath newAp = new AccessPath(rightAp, si).add(startField.getAfter(var).getO1(), startField.getAfter(var).getO2(), si); 
                    taintSet.add(newAp);
                    aliasPath.add(si);
                    flags.setO1(true);
                } else {
                    newPos = goToPrevStaticField(pos);
                    flags.setO2(true);
                }
            } else if (right instanceof FieldRef) {
                if (!matchReferenceVaraibleDefintion(si, startField.getField(), left, right)) {
                    newPos = goToPrevStaticField(pos);
                    flags.setO2(true);
                }
            }
        }
        return newPos;
    }
}
