package ca.ubc.ece.resess.slicer.dynamic.core.graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.InflaterOutputStream;

import ca.ubc.ece.resess.slicer.dynamic.core.graph.sequitur.Rule;
import ca.ubc.ece.resess.slicer.dynamic.core.graph.sequitur.Terminal;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.Statement;
import ca.ubc.ece.resess.slicer.dynamic.core.utils.AnalysisLogger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ca.ubc.ece.resess.slicer.dynamic.core.graph.sequitur.Symbol;


public class Parser {

    private static final String DELEMITER = ":ZZZ:";


    private Parser() {
        throw new IllegalStateException("Utility class");
      }

    public static Trace readFile(String fileName, String staticLogFile) {
        return expandTrace(staticLogFile, fileName);
    }

    public static Trace expandTrace(String staticLogFile, String traceName) {
        Trace listTraces = new Trace();
        Map<Long, List<String>> logMap = new HashMap<>();
        JSONParser parser = new JSONParser();
        try {
            AnalysisLogger.log(true, "Abs path for static-log {}", new File(staticLogFile).getAbsolutePath());
            FileReader reader = new FileReader(staticLogFile);
            Object obj = parser.parse(reader);
            JSONObject jObj = (JSONObject) obj;
            buildLogMap(jObj, logMap);
        } catch (IOException | ParseException e) {
            AnalysisLogger.warn(true, "Cannot read static-log file! {}", e);
        }
        List<String> splitLine = new ArrayList<>();
        String lastSlicingLine = "";
        try (BufferedReader br = new BufferedReader(new FileReader(traceName))) {
            String t;
            while ((t = br.readLine()) != null) {
                try {
                    t = t.split("SLICING:")[1];
                    if (lastSlicingLine.equals(t)) {
                        continue;
                    }
                    lastSlicingLine = t;
                } catch (ArrayIndexOutOfBoundsException e){
                    continue;
                }
                if (t.startsWith(" ZLIB: ")) {
                    t = decompress(t.split(" ZLIB: ")[1]);
                }
                List<String> chunk = Arrays.asList(t.split("-"));
                // AnalysisLogger.log(true, "Chunk before {}", chunk);
                // AnalysisLogger.log(true, "Len before {}", chunk.size());
                // chunk = compressTrace(chunk); // TODO: buggy, need fixing and speeding up before intergartion
                // AnalysisLogger.log(true, "Chunk after {}", chunk);
                // AnalysisLogger.log(true, "Len after {}", chunk.size());
                for (String s: chunk) {
                    int fieldId = -1;
                    long lineNum = -1L;
                    long threadNum = -1L;
                    String [] sSplit = s.split(":");
                    try {
                        lineNum = Long.valueOf(sSplit[0]);
                    } catch (java.lang.NumberFormatException e) {
                        continue;
                    }
                    threadNum = Long.valueOf(sSplit[1]);
                    try {
                        fieldId = Integer.valueOf(sSplit[2]);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // Ignored
                    }
                    addToExpandedTrace(listTraces, logMap, lineNum, threadNum, fieldId);
                }
                AnalysisLogger.log(true, "Expanded trace size: {}", listTraces.size());
            }
        } catch (IOException e) {
            AnalysisLogger.warn(true, "Cannot read trace file! {}", e);
        }
        AnalysisLogger.log(true, "Done parsing");
        return listTraces;
    }

    private static boolean checkIsField(String[] sSplit) {
        boolean isField = false;
        if (sSplit.length > 1 && sSplit[1].equals("FIELD")) {
            isField = true;
        } else {
            isField = false;
        }
        return isField;
    }

    protected static void buildLogMap(JSONObject jObj, Map<Long, List<String>> logMap) {
        for (Object o :jObj.keySet()) {
            String methodName = (String) o;
            JSONObject methodBody = (JSONObject) jObj.get(o);
            for (Object bb :methodBody.keySet()) {
                Long lineNum = Long.valueOf((String) bb);
                Object[] linesInBB = ((JSONArray) methodBody.get(bb)).toArray();
                ArrayList<String> expandedBody = new ArrayList<>();
                for (Object line: linesInBB) {
                    String payload = lineNum + DELEMITER + methodName + DELEMITER + ((String) line);
                    expandedBody.add(payload);
                }
                logMap.put(lineNum, expandedBody);
            }
        }
    }

    protected static void addToExpandedTrace(Trace listTraces, Map<Long, List<String>> logMap, long lineNum,
            long threadNum, int fieldId) {
        try {
            for (String line : logMap.get(lineNum)) {
                line = line + DELEMITER + threadNum + DELEMITER + fieldId;
                String [] tokens = line.split(DELEMITER);
                TraceStatement tr = new TraceStatement();
                if(tokens.length < 4) continue;
                long lineNumber = Long.valueOf(tokens[0]);
                String method = tokens[1];
                String instruction = tokens[2];
                Statement statement = Statement.getStatement(lineNumber, method, instruction);
                tr.setStatement(statement);
                tr.setThreadId(Long.valueOf(tokens[3]));
                if(tokens.length > 4) {
                    tr.setFieldAddr(Long.valueOf(tokens[4]));
                }
                listTraces.add(tr);
            }
        } catch (NullPointerException e) {
            // Ignored
        }
    }

    protected static String decompress(String compressed64) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            byte[] compressed = Base64.getDecoder().decode(compressed64);
            OutputStream ios = new InflaterOutputStream(os);
            ios.write(compressed);
            ios.close();
            os.close();
        } catch (Exception e) {
            AnalysisLogger.warn(true, "Cannot decompress line {}", compressed64);
        }
        byte[] decompressedBArray = os.toByteArray();
        return new String(decompressedBArray);
    }

    private static List<String> compressTrace(List<String> splitLines) {
        Rule firstRule = new Rule();
        int i;
        // Reset number of rules and Hashtable.
        Rule.numRules = 0;
        Map<Symbol, Symbol> diagramTable = new HashMap<>();
        Map<String, String> symbolMap = new HashMap<>();
        for (i = 0; i < splitLines.size(); i++){
          String val = splitLines.get(i).replace(":", "0");
          symbolMap.put(val, splitLines.get(i));
          firstRule.last().insertAfter(new Terminal(Long.valueOf(val)), diagramTable);
          firstRule.last().getP().check(diagramTable);
        }
        AnalysisLogger.log(true, "Rules: {}", firstRule.getRules());
        List<String> compressedString = new ArrayList<>();
        for (String uncompressed: firstRule.getRules().get("R0")) {
            String lastStr = "";
            if (!compressedString.isEmpty()) {
                lastStr = compressedString.get(compressedString.size()-1);
            }
            if (!lastStr.equals(uncompressed)) {
                compressedString.add(uncompressed);
            }
        }

        // AnalysisLogger.log(true, "Compressed: {}", compressedString);

        List<String> resultStr = expand(firstRule, symbolMap, compressedString);
        
        
        // AnalysisLogger.log(true, "Expanded compressed: {}", resultStr);
        return resultStr;
    }

    private static List<String> expand(Rule firstRule, Map<String, String> symbolMap, List<String> compressedString) {
        List<String> resultStr = new ArrayList<>();
        for (String compressed: compressedString) {
            if (compressed.startsWith("R") && firstRule.getRules().containsKey(compressed)) {
                List<String> expandedList = firstRule.getRules().get(compressed);
                for (String str: expandedList) {
                    if (str.startsWith("R")) {
                        resultStr.addAll(expand(firstRule, symbolMap, firstRule.getRules().get(str)));
                    } else {
                        if (symbolMap.get(str) == null) {
                            AnalysisLogger.log(true, "Cannot find str {}", str);
                        } else {
                            resultStr.add(symbolMap.get(str));
                        }
                    }
                }
            } else {
                if (symbolMap.get(compressed) == null) {
                    AnalysisLogger.log(true, "Cannot find comp {}", compressed);
                } else {
                    resultStr.add(symbolMap.get(compressed));
                }
            }
        }
        return resultStr;
    }

}
