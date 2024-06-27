package ca.ubc.ece.resess.slicer.dynamic.core.graph;

import java.io.Serializable;

import ca.ubc.ece.resess.slicer.dynamic.core.statements.Statement;

public class TraceStatement implements Serializable {
    private long fieldAddr = -1L;
    private long threadId;
    private Statement statement;

    @Override
    public String toString() {
        String line = statement.getLineNumber() + ", " + statement.getMethod() + ", " + statement.getInstruction() + ", " + threadId;
        if (fieldAddr != -1L) {
            line += ", FIELD:" + fieldAddr;
        }
        return line;
    }

    public void setStatement(Statement statement) {
      this.statement = statement;
    }

    public void setFieldAddr(Long fieldAddr) {
      this.fieldAddr = fieldAddr;
    }

    public void setThreadId(Long threadId) {
      this.threadId = threadId;
    }

    public Statement getStatement() {
      return statement;
    }

    public Long getLineNumber() {
        return statement.getLineNumber();
    }

    public String getMethod() {
        return statement.getMethod();
    }

    public String getInstruction() {
        return statement.getInstruction();
    }

    public Long getFieldAddr() {
      return fieldAddr;
    }

    public Long getThreadId() {
      return threadId;
    }
}

