package ca.ubc.ece.resess.slicer.dynamic.core.graph.sequitur;

import java.util.Map;

public class Terminal extends Symbol implements Cloneable{
    
    public Terminal(long theValue){
        value = theValue;
        p = null;
        n = null;
    }
    
    public void cleanUp(Map<Symbol, Symbol> digramsTable){
        join(p,n, digramsTable);
        deleteDigram(digramsTable);
    }
}