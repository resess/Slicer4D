package ca.ubc.ece.resess.slicer.dynamic.core.graph;

import java.util.Objects;

public class Edge {


  private int source;
  private int destination;
  private EdgeType edgeType;

  public Edge(int source, int destination) {
    this(source, destination, EdgeType.NONE);
  }

  public Edge(int source, int destination, EdgeType edgeType) {
    this.source = source;
    this.destination = destination;
    this.edgeType = edgeType;
  }

  public int getSource() {
    return source;
  }

  public int getDestination() {
    return destination;
  }

  public EdgeType getEdgeType() {
    return edgeType;
  }

  public void setEdgeType(EdgeType edgeType) {
    this.edgeType = edgeType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, destination, edgeType);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Edge)) {
        return false;
    }
    Edge other = (Edge) obj;
    return source == other.source && destination == other.destination && edgeType.equals(other.edgeType);
  }

  @Override
  public String toString() {
    return String.format("Edge: (%s -> %s: %s)", source, destination, edgeType);
  }

}
