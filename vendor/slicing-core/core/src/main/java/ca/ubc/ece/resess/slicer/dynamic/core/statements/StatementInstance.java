package ca.ubc.ece.resess.slicer.dynamic.core.statements;

import ca.ubc.ece.resess.slicer.dynamic.core.utils.AnalysisUtils;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;

public class StatementInstance {
    
    private Unit u;
    private SootMethod sm;
    private int lineNo = -1; // Index in the trace
    private StatementMap returnChunk;
    private long threadID = -1L;
    private long fieldId = -1L;
    private int javaSourceLineNo = -1;
    private String javaSourceFile = "UNKNOWN";


    public StatementInstance(SootMethod sm, Unit u, int lineNo, Long tid, Long fieldId, Integer javaSourceLineNo, String javaSourceFile) {
        this.sm = sm;
        this.u = u;
        this.lineNo = lineNo;
        if(tid != null){
            this.threadID = tid;
        } 
        if ((u instanceof AssignStmt) && ((AssignStmt) u).containsFieldRef()) {
            this.fieldId = fieldId;
        }
        this.javaSourceLineNo = javaSourceLineNo;
        this.javaSourceFile = javaSourceFile;
    }

    public Type getClassType() {
        return this.sm.getDeclaringClass().getType();
    }

    public Unit getUnit() {
        return u;
    }

    /**
     * Get the index in the trace
     * @return index in the trace
     */
    public int getLineNo() {
        return lineNo;
    }

    public Long getThreadID() {
        return threadID;
    }

    public SootMethod getMethod(){
        return sm;
    }

    public SootMethod getCalledMethod() {
        if (((Stmt) u).containsInvokeExpr()) {
            return ((Stmt) u).getInvokeExpr().getMethod();
        } 
        return null;
    }

    public Integer getJavaSourceLineNo() {
        return javaSourceLineNo;
    }

    public String getJavaSourceFile() {
        return javaSourceFile;
    }

    public String getUnitId() {
        StringBuilder sb = new StringBuilder();
        sb.append(lineNo);
        sb.append(", ");
        sb.append(sm.getSubSignature().replace(",", ";"));
        sb.append(", ");
        sb.append(sm.getDeclaringClass().getName());
        sb.append(", ");
        sb.append((u==null? "null":u.toString().replace(",", ";")));
        sb.append((fieldId==-1L? "":":FIELD:"+fieldId));
        sb.append((javaSourceLineNo==-1? "":":LINENO:"+javaSourceLineNo));
        sb.append((javaSourceFile.equals("")? "":":FILE:"+javaSourceFile));
        return sb.toString();
    }

    @Override
    public String toString() {
        return getUnitId();
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + u.hashCode();
        result = 31 * result + sm.hashCode();
        result = 31 * result + lineNo;
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof StatementInstance)) {
            return false;
        }
        StatementInstance otherUnit = (StatementInstance) other;
        return otherUnit.u.equals(u) &&
               otherUnit.lineNo == lineNo &&
               otherUnit.sm.equals(sm);
    }
    
    public boolean isReturn(){
        return (this.u instanceof ReturnStmt) || (this.u instanceof ReturnVoidStmt);
    }

    public StatementMap getReturnChunk() {
        return returnChunk;
    }

    public void setReturnChunk(StatementMap returnChunk) {
        this.returnChunk = returnChunk;
    }

    public void setThreadID(Long threadID) {
        this.threadID = threadID;
    }

    public boolean classEquals(StatementInstance other) {
        return this.sm.getDeclaringClass().equals(other.getMethod().getDeclaringClass());
    }

    public boolean methodEquals(StatementInstance other) {
        return this.sm.equals(other.sm);
    }

    public String getReturnVar() {
        return ((ReturnStmt) this.u).getOp().toString();
    }

    public Type getReturnType() {
        return ((ReturnStmt) this.u).getOp().getType();
    }

    public String getThrowVar() {
        return ((ThrowStmt) this.u).getOp().toString();
    }

    public Type getThrowType() {
        return ((ThrowStmt) this.u).getOp().getType();
    }

    public Long getFieldId() {
        return fieldId;
    }

    public boolean hasPossibleSetter(){
        if ((u instanceof Stmt) && (((Stmt)u).containsInvokeExpr())) {
            InvokeExpr expr = AnalysisUtils.getCallerExp(this);
            if (expr.getMethod().getName().startsWith("set")) {
                return true;
            }
        }
        return false;
    }

    public boolean containsInvokeExpr(){
        if ((u instanceof Stmt) && (((Stmt)u).containsInvokeExpr())) {
            return true;
        }
        return false;
    }

    public boolean isAssign() {
        if (this.u instanceof AssignStmt) {
            return true;
        }
        return false;
    }

    public boolean isAfter(StatementInstance other) {
        return (other.getLineNo() < this.getLineNo()) || other.getMethod().getName().equals("<clinit>");
    }

    public String getUnitString() {
        return this.u.toString();
    }
}
