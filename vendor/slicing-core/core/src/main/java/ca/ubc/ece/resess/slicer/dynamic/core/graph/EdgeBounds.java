package ca.ubc.ece.resess.slicer.dynamic.core.graph;

import java.util.Objects;

public class EdgeBounds {
  private int source;
  private int destination;

  public EdgeBounds(int s, int d) {
    this.source = s;
    this.destination = d;
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, destination);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj instanceof EdgeBounds) {
      EdgeBounds other = (EdgeBounds) obj;
      return this.source == other.source && this.destination == other.destination;
    }
    return false;
  }
}
