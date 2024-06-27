package ca.ubc.ece.resess.slicer.dynamic.core.utils;

import java.util.Map;
import java.util.Set;

import ca.ubc.ece.resess.slicer.dynamic.core.accesspath.AccessPath;
import ca.ubc.ece.resess.slicer.dynamic.core.accesspath.AliasSet;
import ca.ubc.ece.resess.slicer.dynamic.core.graph.CalledChunk;
import ca.ubc.ece.resess.slicer.dynamic.core.graph.CallerContext;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementInstance;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementMap;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementSet;

import java.util.HashMap;


import soot.toolkits.scalar.Pair;

public class AnalysisCache {
    
    private Map<Pair<StatementInstance, Integer>, AliasSet> aliasAnalysisCache = new HashMap<>();
    private Map<Integer, Integer> callerCache = new HashMap<>();
    private Map<Integer, StatementMap> bwChunkCache = new HashMap<>();
    private Map<Integer, StatementMap> fwChunkCache = new HashMap<>();
    private Map<Pair<StatementInstance, AccessPath>, Set<CallerContext>> nextCallbackCache = new HashMap<>();
    private Map<StatementInstance, CallerContext> callerForwardChunk = new HashMap<>();
    private Map<StatementInstance, StatementInstance> previousCallbackCache = new HashMap<>();
    private Map<StatementInstance, CalledChunk> calledChunkCache = new HashMap<>();
    private Map<Pair<StatementMap, StatementInstance>, StatementMap> inTraceOrderChunkCache = new HashMap<>();
    
    public AnalysisCache() {
        aliasAnalysisCache = new HashMap<>();
        callerCache = new HashMap<>();
        bwChunkCache = new HashMap<>();
        fwChunkCache = new HashMap<>();
        nextCallbackCache = new HashMap<>();
        callerForwardChunk = new HashMap<>();
        previousCallbackCache = new HashMap<>();
        calledChunkCache = new HashMap<>();
        inTraceOrderChunkCache = new HashMap<>();
    }
    
    public void reset() {
        aliasAnalysisCache = new HashMap<>();
        callerCache = new HashMap<>();
        bwChunkCache = new HashMap<>();
        fwChunkCache = new HashMap<>();
        nextCallbackCache = new HashMap<>();
        callerForwardChunk = new HashMap<>();
        previousCallbackCache = new HashMap<>();
        calledChunkCache = new HashMap<>();
        inTraceOrderChunkCache = new HashMap<>();
    }
    
    public synchronized void putInInTraceOrderChunkCache(StatementMap k1, StatementInstance k2, StatementMap v) {
        inTraceOrderChunkCache.put(new Pair<>(k1, k2), v);
    }
    
    public StatementMap getFromInTraceOrderChunkCache(StatementMap k1, StatementInstance k2){
        return inTraceOrderChunkCache.get(new Pair<>(k1, k2));
    }
    
    public synchronized void putInCalledChunkCache(StatementInstance iu, CalledChunk cc){
        calledChunkCache.put(iu, cc);
    }
    
    public CalledChunk getFromCalledChunkCache(StatementInstance iu){
        return calledChunkCache.get(iu);
    }
    
    public StatementInstance getFromPreviousCallbackCache(StatementInstance k) {
        return previousCallbackCache.get(k);
    }
    
    public synchronized void putInPreviousCallbackCache(StatementInstance k, StatementInstance v) {
        previousCallbackCache.put(k, v);
    }
    
    public CallerContext getFromCallerForwardChunk(StatementInstance key){
        return callerForwardChunk.get(key);
    }
    
    public synchronized void putInCallerForwardChunk(StatementInstance key, CallerContext value){
        callerForwardChunk.put(key, value);
    }
    
    public Set<CallerContext> getFromNextCallbackCache(StatementInstance k, AccessPath ap) {
        return nextCallbackCache.get(new Pair<>(k, ap));
    }
    
    public synchronized void putInNextCallbackCache(StatementInstance k, AccessPath ap, Set<CallerContext> v){
        nextCallbackCache.put(new Pair<>(k, ap), v);
    }
    
    public Integer getFromCallerCache(int pos) {
        return callerCache.get(pos);
    }
    
    public synchronized void putInCallerCache(int pos, int callerPos) {
        callerCache.put(pos, callerPos);
    }
    
    public StatementMap getFromBwChunkCache(int pos) {
        return bwChunkCache.get(pos);
    }
    
    public synchronized void putInBwChunkCache(int pos, StatementMap chunk) {
        bwChunkCache.put(pos, chunk);
    }
    
    public StatementMap getFromFwChunkCache(int pos) {
        return fwChunkCache.get(pos);
    }
    
    public synchronized void putInFwChunkCache(int pos, StatementMap chunk) {
        fwChunkCache.put(pos, chunk);
    }
    
    public synchronized AliasSet filterByAnalysisCache(StatementInstance iu, AliasSet as, Integer direction){
        Pair<StatementInstance, Integer> key = new Pair<>(iu, direction);
        if (!aliasAnalysisCache.containsKey(key)) {
            aliasAnalysisCache.put(key, as);
            return as;
        }
        AliasSet removedSet = new AliasSet();
        AliasSet cachedSet = aliasAnalysisCache.get(key);
        for (AccessPath ap: as) {
            for (AccessPath cached: cachedSet) {
                if (cached.pathEquals(ap)) {
                    removedSet.add(ap);
                }
            }
        }
        AliasSet toAdd = as.subtract(removedSet);
        cachedSet.addAll(as);
        aliasAnalysisCache.put(key, cachedSet);
        return toAdd;
    }
}