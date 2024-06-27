package ca.ubc.ece.resess.slicer.dynamic.core.graph.sequitur;

import java.util.Map;


public abstract class Symbol {
    
    static final int numTerminals = 100000;
    
    static final int prime = 2265539;
    
    public long value;
    public Symbol p;
    public Symbol n;
    
    /**
    * Links two symbols together, removing any old
    * digram from the hash table.
    */
    
    public static void join(Symbol left, Symbol right, Map<Symbol, Symbol> digramsTable){
        if (left.n != null) {
            left.deleteDigram(digramsTable);
        }
        left.n = right;
        right.p = left;
    }
    
    public Symbol getN() {
        return n;
    }

    public Symbol getP() {
        return p;
    }

    /**
    * Abstract method: cleans up for symbol deletion.
    */
    
    public abstract void cleanUp(Map<Symbol, Symbol> digramsTable);
    
    /**
    * Inserts a symbol after this one.
    */
    
    public void insertAfter(Symbol toInsert, Map<Symbol, Symbol> digramsTable){
        join(toInsert, n, digramsTable);
        join(this, toInsert, digramsTable);
    }
    
    /**
    * Removes the digram from the hash table.
    * Overwritten in sub class guard.
    */
    
    public void deleteDigram(Map<Symbol, Symbol> digramsTable){
        
        Symbol dummy;
        
        if (n.isGuard()) {
            return;
        }
        
        dummy = digramsTable.get(this);
        
        // Only delete digram if its exactly
        // the stored one.
        if (dummy == this) {
            digramsTable.remove(this);
        }
    }
    
    /**
    * Returns true if this is the guard symbol.
    * Overwritten in subclass guard.
    */
    
    public boolean isGuard(){
        return false;
    }
    
    /**
    * Returns true if this is a non-terminal.
    * Overwritten in subclass nonTerminal.
    */
    
    public boolean isNonTerminal(){
        return false;
    }
    
    /**
    * Checks a new digram. If it appears
    * elsewhere, deals with it by calling
    * match(), otherwise inserts it into the
    * hash table.
    * Overwritten in subclass guard.
    */
    
    public boolean check(Map<Symbol, Symbol> digramsTable){
        
        Symbol found;
        
        if (n.isGuard()) {
            return false;
        }
        
        if (!digramsTable.containsKey(this)){
            digramsTable.put(this,this);
            return false;
        }
        found = digramsTable.get(this);
        if (found.n != this) {
            match(this, found, digramsTable);
        }
        return true;
    }
    
    /**
    * Replace a digram with a non-terminal.
    */
    
    public void substitute(Rule r, Map<Symbol, Symbol> digramsTable){
        cleanUp(digramsTable);
        n.cleanUp(digramsTable);
        p.insertAfter(new NonTerminal(r), digramsTable);
        if (!p.check(digramsTable)) {
            p.n.check(digramsTable);
        }
        
    }
    
    /**
    * Deal with a matching digram.
    */
    
    public void match(Symbol newD, Symbol matching, Map<Symbol, Symbol> digramsTable){
        
        Rule r;
        Symbol first, second, dummy;
        
        if (matching.p.isGuard() &&  matching.n.n.isGuard()){
            
            // reuse an existing rule
            
            r = ((Guard) matching.p).r;
            newD.substitute(r, digramsTable);
        }else{
            
            // create a new rule
            
            r = new Rule();
            try{
                first = (Symbol)newD.clone();
                second = (Symbol)newD.n.clone();
                r.theGuard.n = first;
                first.p = r.theGuard;
                first.n = second;
                second.p = first;
                second.n = r.theGuard;
                r.theGuard.p = second;
                
                digramsTable.put(first,first);
                matching.substitute(r, digramsTable);
                newD.substitute(r, digramsTable);
            }catch (CloneNotSupportedException c){
                c.printStackTrace();
            }
        }
        
        // Check for an underused rule.
        
        if (r.first().isNonTerminal() &&  (((NonTerminal)r.first()).r.count == 1)) {
            ((NonTerminal)r.first()).expand(digramsTable);
        }
    }
    
    /**
    * Produce the hashcode for a digram.
    */
    
    public int hashCode(){
        
        long code;
        
        // Values in linear combination with two
        // prime numbers.
        
        code = ((21599*(long)value)+(20507*(long)n.value));
        code = code%(long)prime;
        return (int)code;
    }
    
    /**
    * Test if two digrams are equal.
    * WARNING: don't use to compare two symbols.
    */
    
    public boolean equals(Object obj){
        return ((value == ((Symbol)obj).value) &&
        (n.value == ((Symbol)obj).n.value));
    }
}
