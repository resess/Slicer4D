package ca.ubc.ece.resess.slicer.dynamic.core.graph.sequitur;

import java.util.Map;

public class NonTerminal extends Symbol implements Cloneable{

    Rule r;
  
    public NonTerminal(Rule theRule){
      r = theRule;
      r.count++;
      value = numTerminals+r.number;
      p = null;
      n = null;
    }
  
    /**
     * Extra cloning method necessary so that
     * count in the corresponding rule is
     * increased.
     */
  
    protected NonTerminal clone(){
      NonTerminal sym = new NonTerminal(r);
      sym.p = p;
      sym.n = n;
      return sym;
    }
  
    public void cleanUp(Map<Symbol, Symbol> digramsTable){
      join(p, n, digramsTable);
      deleteDigram(digramsTable);
      r.count--;
    }
  
    public boolean isNonTerminal(){
      return true;
    }
  
    /**
     * This symbol is the last reference to
     * its rule. The contents of the rule
     * are substituted in its place.
     */
  
    public void expand(Map<Symbol, Symbol> digramsTable){
      join(p,r.first(),digramsTable);
      join(r.last(),n,digramsTable);
  
      // Necessary so that garbage collector
      // can delete rule and guard.
  
      r.theGuard.r = null;
      r.theGuard = null;
    }
  }