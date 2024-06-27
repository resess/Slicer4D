package ca.ubc.ece.resess.slicer.dynamic.core.graph.sequitur;

import java.util.Map;

public class Guard extends Symbol{

    Rule r;
  
    public Guard(Rule theRule){
      r = theRule;
      value = 0;
      p = this;
      n = this;
    }
  
    @Override
    public void cleanUp(Map<Symbol, Symbol> digramsTable){
      join(p, n, digramsTable);
    }
  

    @Override
    public boolean isGuard(){
      return true;
    }
  
    @Override
    public void deleteDigram(Map<Symbol, Symbol> digramsTable){
      
      // Do nothing
    }
    
    @Override
    public boolean check(Map<Symbol, Symbol> digramsTable){
      return false;
    }
  }