//package ca.ubc.ece.resess.trace;
//
//import guru.nidi.graphviz.attribute.Label;
//import guru.nidi.graphviz.engine.Graphviz;
//import guru.nidi.graphviz.engine.Rasterizer;
//import guru.nidi.graphviz.model.MutableGraph;
//import guru.nidi.graphviz.model.MutableNode;
//import guru.nidi.graphviz.parse.Parser;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.jupiter.api.parallel.Execution;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.List;
//
//import static guru.nidi.graphviz.model.Factory.mutGraph;
//import static guru.nidi.graphviz.model.Factory.mutNode;
//import static org.junit.Assert.*;
//import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
//
//@Execution(SAME_THREAD)
//public class SubGraphBuilderTest {
//
//    public final Path tempDir = Files.createTempDirectory("debuggerpp-subgraph-test-");
//    public final File subGraphFile = tempDir.resolve("slice-subgraph.png").toFile();
//    public final File subGraphDotFile = tempDir.resolve("slice-subgraph.dot").toFile();
//    public final File graphDotFile = tempDir.resolve("slice-graph.dot").toFile();
//    public final File sliceLogFile = tempDir.resolve("slice.log").toFile();
//
//    public SubGraphBuilderTest() throws IOException {
//    }
//
//    @Before
//    public void setUp() throws IOException {
//        // Write sample slice log data to sliceLogFilePath
//        List<String> lines = List.of(
//                "Main:5",
//                "Main:6",
//                "Main:9",
//                "Main:14"
//        );
//        try {
//            Files.write(sliceLogFile.toPath(), lines);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        // Build a sample graph and write it to subGraphDotFilePath
//        MutableGraph g = mutGraph("example").setDirected(true);
//        MutableNode node1 = mutNode("Main:5:").add(Label.of("Main:5:"));
//        MutableNode node2 = mutNode("Main:6:").add(Label.of("Main:6:"));
//        MutableNode node3 = mutNode("Main:9:").add(Label.of("Main:9:"));
//        MutableNode node4 = mutNode("Main:14:").add(Label.of("Main:14:"));
//
//        g.add(node1);
//        g.add(node2);
//        g.add(node3);
//        g.add(node4);
//        g.addLink(mutNode("Main:5:").addLink(mutNode("Main:6:")).addLink(mutNode("Main:9:")).addLink(mutNode("Main:14:")));
//        Graphviz.fromGraph(g).rasterize(Rasterizer.builtIn("dot")).toFile(graphDotFile);
//    }
//
//    @Test
//    // Testing that the subgraph file is generated successfully for a valid line number.
//    public void testGenerateSubGraph() throws IOException {
//        SubGraphBuilder subGraphBuilder = new SubGraphBuilder();
//        subGraphBuilder.generateSubGraph(14, graphDotFile, sliceLogFile, subGraphFile, subGraphDotFile);
//        assertTrue(subGraphFile.exists());
//        assertTrue(subGraphFile.length() > 0);
//    }
//
//    @Test
//    // Testing that the subgraph file is not generated for an invalid line number (negative value).
//    public void testGenerateSubGraphWithInvalidLine() throws IOException {
//        SubGraphBuilder subGraphBuilder = new SubGraphBuilder();
//        subGraphBuilder.generateSubGraph(-1, graphDotFile, sliceLogFile, subGraphFile, subGraphDotFile);
//        assertFalse(subGraphFile.exists());
//    }
//
//    @Test
//    // Testing that the subgraph file is not generated for an invalid line number (not in slice).
//    public void testGenerateSubGraphWithInvalidLine2() throws IOException {
//        SubGraphBuilder subGraphBuilder = new SubGraphBuilder();
//        subGraphBuilder.generateSubGraph(3, graphDotFile, sliceLogFile, subGraphFile, subGraphDotFile);
//        assertFalse(subGraphFile.exists());
//    }
//
//    @Test
//    // Check if the number of nodes in the subgraph is correct. Current lineNum is 5.
//    public void testGenerateSubGraphNodeNum1() throws IOException {
//        SubGraphBuilder subGraphBuilder = new SubGraphBuilder();
//        subGraphBuilder.generateSubGraph(5, graphDotFile, sliceLogFile, subGraphFile, subGraphDotFile);
//        Parser parser = new Parser();
//        MutableGraph g = parser.read(subGraphDotFile);
//        assertEquals(1, g.nodes().size());
//    }
//
//    @Test
//    // Check if the number of nodes in the subgraph is correct. Current lineNum is 6.
//    public void testGenerateSubGraphNodeNum2() throws IOException {
//        SubGraphBuilder subGraphBuilder = new SubGraphBuilder();
//        subGraphBuilder.generateSubGraph(6, graphDotFile, sliceLogFile, subGraphFile, subGraphDotFile);
//        Parser parser = new Parser();
//        MutableGraph g = parser.read(subGraphDotFile);
//        assertEquals(2, g.nodes().size());
//    }
//
//    @Test
//    // Check if the number of nodes in the subgraph is correct. Current lineNum is 9.
//    public void testGenerateSubGraphNodeNum3() throws IOException {
//        SubGraphBuilder subGraphBuilder = new SubGraphBuilder();
//        subGraphBuilder.generateSubGraph(9, graphDotFile, sliceLogFile, subGraphFile, subGraphDotFile);
//        Parser parser = new Parser();
//        MutableGraph g = parser.read(subGraphDotFile);
//        assertEquals(3, g.nodes().size());
//    }
//
//    @Test
//    // Check if the number of nodes in the subgraph is correct. Current lineNum is 14.
//    public void testGenerateSubGraphNodeNum4() throws IOException {
//        SubGraphBuilder subGraphBuilder = new SubGraphBuilder();
//        subGraphBuilder.generateSubGraph(14, graphDotFile, sliceLogFile, subGraphFile, subGraphDotFile);
//        Parser parser = new Parser();
//        MutableGraph g = parser.read(subGraphDotFile);
//        assertEquals(4, g.nodes().size());
//    }
//}