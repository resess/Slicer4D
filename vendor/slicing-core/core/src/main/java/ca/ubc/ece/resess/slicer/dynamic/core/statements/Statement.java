package ca.ubc.ece.resess.slicer.dynamic.core.statements;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Statement implements Serializable {
  private long lineNumber;
  private String method;
  private String instruction;

  private static Map<String, Statement> createdStatements = new HashMap<>();

  public Statement(Long lineNumber, String method, String instruction) {
    this.lineNumber = lineNumber;
    this.method = method;
    this.instruction = instruction;
  }

  public Long getLineNumber() {
    return lineNumber;
  }

  public String getMethod() {
    return method;
  }

  public String getInstruction() {
    return instruction;
  }

  @Override
  public String toString() {
    return String.valueOf(lineNumber) + ", " + method + ", " + instruction;
  }

  public static Statement getStatement(Long lineNumber, String method, String instruction){
    Statement statement = new Statement(lineNumber, method, instruction);
    if (createdStatements.containsKey(statement.toString())) {
      statement = createdStatements.get(statement.toString());
      if (statement.getLineNumber().equals(lineNumber) && statement.getMethod().equals(method) && statement.getInstruction().equals(instruction)) {
        return statement;
      } else {
        throw new BadStatementMappingException(
          String.format("New statement (%s, %s, %s) does not match stored statement (%s, %s, %s)", 
          lineNumber, method, instruction, statement.getLineNumber(), statement.getMethod(), statement.getInstruction()));
      }
    }
    createdStatements.put(statement.toString(), statement);
    return statement;
  }
}
