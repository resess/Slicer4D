package ca.ubc.ece.resess.slicer.dynamic.core.exceptions;

public class TraceFileException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    public TraceFileException () {
        super("Execption p[arsing trace file", new Throwable());
    }
}
