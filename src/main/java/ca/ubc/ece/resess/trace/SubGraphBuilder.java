package ca.ubc.ece.resess.trace;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.Rasterizer;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import guru.nidi.graphviz.parse.Parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static guru.nidi.graphviz.attribute.Color.TRANSPARENT;
import static guru.nidi.graphviz.model.Factory.mutGraph;

public class SubGraphBuilder {
    // TODO: does this support multiple files?
    public void generateSubGraph(int currentLine, File dotGraphFile, File sliceLogFile, File outputPngFile, File outputDotFile) throws IOException {
        Parser parser = new Parser();
        // Read the full graph
        MutableGraph g = parser.read(dotGraphFile);
        MutableGraph subGraph = mutGraph("Subgraph").setDirected(true);
        System.out.println("Reading dot file success");

        // Get the list of lines in the slice
        List<Integer> sliceLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(sliceLogFile))) {
            String line;
            Pattern pattern = Pattern.compile("([a-zA-Z]+):(\\d+)");
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    sliceLines.add(Integer.parseInt(matcher.group(2)));
                }
            }
        } catch (IOException e) {
            System.out.println("An error occurred while reading the log file: " + e.getMessage());
            return;
        }

        // Check if currentLine is valid (it should appear in sliceLines)
        boolean isValid = false;
        for (Integer sliceLine : sliceLines) {
            if (Objects.equals(sliceLine, currentLine)) {
                isValid = true;
                break;
            }
        }
        if (!isValid) {
            System.out.println("Line is not valid!");
            return;
        }

        // Loop through the nodes in the graph. For each node, check if the line# appears before the current line in sliceLines
        // If it does, keep it; if not, remove the node and corresponding links
        // FIXME: slice.log might be unordered
        for (MutableNode node : g.nodes()) {
//            System.out.println("Node name :" + node.name());
            String nodeName = String.valueOf(node.name());
            Pattern pattern = Pattern.compile("([a-zA-Z]+):(\\d+):");
            Matcher matcher = pattern.matcher(nodeName);
//            System.out.println("Node name is: " + nodeName);
            if (matcher.find()) {
                Integer NodeLineNum = Integer.parseInt(matcher.group(2));
//                System.out.println("Current node number is: " + NodeLineNum);
                for (Integer sliceLine : sliceLines) {
                    if (Objects.equals(sliceLine, NodeLineNum)) {
                        subGraph.add(node);
                        break;
                    }
                    if (Objects.equals(sliceLine, currentLine)) {
                        break;
                    }
                }
            }
        }

        if (subGraph.nodes().isEmpty()) {
            System.out.println("Subgraph is empty!");
            return;
        }

        // The code below is for reversing every link in the subgraph
        // Create a list to store the reversed links
        ArrayList<Link> reversedLinks = new ArrayList<>();
        // Create a list to store the names of the target nodes of the links
        ArrayList<String> targetNodeNames = new ArrayList<>();

        // Iterate over all nodes in the graph
        for (MutableNode node : subGraph.nodes()) {
            // Iterate over all links of the current node
            for (Link link : node.links()) {
                // Create a new link with reversed direction and add it to the list
                reversedLinks.add(Link.to(node).with(link.attrs()));
                targetNodeNames.add(link.to().name().toString());
            }
            // Remove all links from the current node
            node.links().clear();
        }

        // Add all reversed links to their target nodes
        for (int i = 0; i < reversedLinks.size(); i++) {
            for (MutableNode targetNode : subGraph.nodes()) {
                if (targetNode.name().toString().equals(targetNodeNames.get(i))) {
//                    System.out.println("name matched! - " + targetNodeNames.get(i));
                    targetNode.addLink(reversedLinks.get(i));
                }
            }
        }
        
        subGraph.nodeAttrs().add(Color.WHITE).linkAttrs().add(Color.WHITE).graphAttrs().add(Color.WHITE);
        subGraph.nodeAttrs().add(Color.WHITE.font()).linkAttrs().add(Color.WHITE.font());
        subGraph.graphAttrs().add(TRANSPARENT.background());
        Graphviz.fromGraph(subGraph).width(1200).render(Format.PNG).toFile(outputPngFile);
        Graphviz.fromGraph(subGraph).rasterize(Rasterizer.builtIn("dot")).toFile(outputDotFile);
    }
}