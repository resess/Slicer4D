package ca.ubc.ece.resess.slicer.dynamic.core.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Graph {
  private Map<Integer, List<Edge>> edgeMapFromSource;
  private Map<Integer, List<Edge>> edgeMapFromDestination;

  public Graph() {
    edgeMapFromSource = new HashMap<>();
    edgeMapFromDestination = new HashMap<>();
  }

  public void setEdgeType(int source, int destination, EdgeType edgeType) {
    Edge edge = new Edge(source, destination, edgeType);
    List<Edge> mappedEdges = edgeMapFromSource.getOrDefault(source, new ArrayList<>());
    updateOrAddEdge(destination, edgeType, edge, mappedEdges);
    edgeMapFromSource.put(source, mappedEdges);

    mappedEdges = edgeMapFromDestination.getOrDefault(destination, new ArrayList<>());
    updateOrAddEdge(destination, edgeType, edge, mappedEdges);
    edgeMapFromDestination.put(destination, mappedEdges);
  }

  private void updateOrAddEdge(int destination, EdgeType edgeType, Edge edge, List<Edge> mappedEdges) {
    boolean set = false;
    for (Edge me: mappedEdges) {
      if (me.getDestination() == destination) {
        me.setEdgeType(edgeType);
        set = true;
      }
    }
    if (!set) {
      mappedEdges.add(edge);
    }
  }

  public Set<Map.Entry<Integer, List<Edge>>> getEdgeSet() {
    return edgeMapFromSource.entrySet();
  }

  public void removeAllEdges(Set<Edge> edges) {
    for (Edge edge: edges) {
      List<Edge> mappedEdges = edgeMapFromSource.getOrDefault(edge.getSource(), new ArrayList<>());
      mappedEdges.remove(edge);
      edgeMapFromSource.put(edge.getSource(), mappedEdges);

      mappedEdges = edgeMapFromDestination.getOrDefault(edge.getDestination(), new ArrayList<>());
      mappedEdges.remove(edge);
      edgeMapFromDestination.put(edge.getDestination(), mappedEdges);
    }

  }

  public List<Integer> predecessorListOf(int vertex) {
    Set<Integer> preds = new HashSet<>();
    if (edgeMapFromDestination.containsKey(vertex)) {
      edgeMapFromDestination.get(vertex).forEach(v -> preds.add(v.getSource()));
    }
    return new ArrayList<>(preds);
  }

  public List<Integer> successorListOf(int vertex) {
    Set<Integer> successors = new HashSet<>();
    if (edgeMapFromSource.containsKey(vertex)) {
      edgeMapFromSource.get(vertex).forEach(v -> successors.add(v.getDestination()));
    }
    return new ArrayList<>(successors);
  }

  public Edge getEdge(int source, int destination) {
    if (!edgeMapFromSource.containsKey(source)) {
      return null;
    }
    for (Edge edge : edgeMapFromSource.get(source)) {
      if (edge.getDestination() == destination) {
        return edge;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    if (edgeMapFromSource.isEmpty()) {
      return "Empty";
    }
    StringBuilder sb = new StringBuilder();
    for (List<Edge> edgeList: edgeMapFromSource.values()) {
      Iterator<Edge> it = edgeList.iterator();
      while (it.hasNext()) {
        Edge e = it.next();
        sb.append("\n");
        sb.append(e.toString());
      }
    }
    return sb.toString();
  }
}
