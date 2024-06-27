package ca.ubc.ece.resess.slicer.dynamic.core.controldependence;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


import ca.ubc.ece.resess.slicer.dynamic.core.graph.DynamicControlFlowGraph;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementInstance;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementMap;
import ca.ubc.ece.resess.slicer.dynamic.core.utils.AnalysisLogger;
import soot.toolkits.graph.pdg.EnhancedUnitGraph;
import soot.toolkits.graph.pdg.Region;
import soot.toolkits.graph.pdg.RegionAnalysis;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.GotoStmt;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ControlDominator{

    static Set<SootMethod> outOfMemMethods = new LinkedHashSet<>();
    static Map<SootMethod, EnhancedUnitGraph> computedGraphs = new LinkedHashMap<>();
    private ControlDominator() {
        throw new IllegalStateException("Utility class");
    }

    static int getIntFromString(String s) {
        Matcher matcher = Pattern.compile("\\d+").matcher(s);
        matcher.find();
        return Integer.valueOf(matcher.group());
    }

    public static StatementInstance getControlDominator(StatementInstance stmt, StatementMap chunk, DynamicControlFlowGraph dcfg){
        if (outOfMemMethods.contains(stmt.getMethod())) {
            return null;
        }
        ControlDomRunner cdr = new ControlDomRunner(stmt, chunk, dcfg);
        Thread t = new Thread(cdr);
        try {
            t.start();
            t.join(30*1000);
            t.interrupt();
        } catch (InterruptedException e) {
            // pass
        }
        return cdr.getCandidateIu();
    }


    static class ControlDomRunner implements Runnable {
        private final StatementInstance stmt;
        private final StatementMap chunk;
        private final DynamicControlFlowGraph dcfg;
        private StatementInstance candidateIu;

        ControlDomRunner (StatementInstance stmt, StatementMap chunk, DynamicControlFlowGraph dcfg){
            this.stmt = stmt;
            this.chunk = chunk;
            this.dcfg = dcfg;
        }
        @Override
        public void run() {
            this.candidateIu = getControlDom(stmt, chunk, dcfg);
        }

        public StatementInstance getCandidateIu() {
            return candidateIu;
        }
    }


    private static StatementInstance getControlDom(StatementInstance stmt, StatementMap chunk, DynamicControlFlowGraph icdg) {
        StatementInstance candidateIu = null;
        try {
            EnhancedUnitGraph cug;
            if (computedGraphs.containsKey(stmt.getMethod())) {
                cug = computedGraphs.get(stmt.getMethod());
            } else {
                cug = new EnhancedUnitGraph(stmt.getMethod().getActiveBody());
                computedGraphs.put(stmt.getMethod(), cug);
            }
            
            // AnalysisLogger.warn(true, "Graph: {}", cug);
            // HashMutablePDG pdg = new HashMutablePDG(new ExceptionalUnitGraph(stmt.getMethod().getActiveBody()));
            RegionAnalysis ra = new RegionAnalysis(cug, stmt.getMethod(), stmt.getMethod().getDeclaringClass());
            for(Region r: ra.getRegions()) {
                List<Unit> regionUnits = r.getUnits();
                if (regionUnits.contains(stmt.getUnit())) {
                    if (regionUnits.toString().contains(":= @caughtexception") || regionUnits.toString().contains("goto [?= throw")) {
                        candidateIu = lineBeforeException(icdg, regionUnits, stmt);
                    } else {
                        candidateIu = matchControlDom(stmt, chunk, cug, ra.getRegions(), r);
                    }
                }
            }
        } catch (OutOfMemoryError e1) {
            AnalysisLogger.warn(true, "Could not compute control dom due to OutOfMemoryError");
            outOfMemMethods.add(stmt.getMethod());
        } catch (Exception e2) {
            AnalysisLogger.warn(true, "Could not compute control dom due to exception: {}", e2);
        }
        return candidateIu;
    }


    private static StatementInstance matchControlDom(StatementInstance stmt, StatementMap chunk, EnhancedUnitGraph methodGraph, List<Region> allRegions, Region region) {
        StatementInstance candidateIu = null;
        Unit first = region.getFirst();
        List<Unit> preds = methodGraph.getPredsOf(first);
        for (Unit pred : preds) {
            candidateIu = compareUnits(stmt, chunk, pred);
        }
        return candidateIu;
    }

    private static StatementInstance compareUnits(StatementInstance stmt, StatementMap chunk, Unit unit) {
        for (StatementInstance iu: chunk.values()) {
            if (iu.getLineNo() > stmt.getLineNo()) {
                continue;
            }
            if (iu.getUnit().equals(unit)) {
                return iu;
            }
        }
        return null;
    }


    private static StatementInstance lineBeforeException(DynamicControlFlowGraph icdg, List<Unit> regionUnits, StatementInstance statementInstance) {
        StatementInstance prev = null;
        Set<Unit> mustFind = new HashSet<>();
        for (Unit u: regionUnits) {
            if (u.equals(statementInstance.getUnit())) {
                break;
            }
            if (!u.toString().contains(":= @caughtexception")) {
                mustFind.add(u);
            }
        }
        int newPos = statementInstance.getLineNo();
        while (newPos > 0) {
            newPos--;
            prev = icdg.mapNoUnits(newPos);
            if ((prev!=null) && !((prev.getUnit()) instanceof GotoStmt)) {
                if (mustFind.isEmpty()) {
                    return prev;
                } else if (mustFind.contains(prev.getUnit())) {
                    mustFind.remove(prev.getUnit());
                }
            }
            
        }
        return prev;
    }
}