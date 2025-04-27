package org.example.planning;


import org.example.agent.Position;
import java.util.*;

public class GraphNode {
    private final Position position;
    private final Map<GraphNode, Double> neighbors = new HashMap<>();
    private boolean hasObstacle = false;

    public GraphNode(Position position) {
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }

    public Map<GraphNode, Double> getNeighbors() {
        return neighbors;
    }

    public void addNeighbor(GraphNode neighbor, double cost) {
        neighbors.put(neighbor, cost);
    }
    public void setObstacle(boolean obstacle) {
        this.hasObstacle = obstacle;
    }

    public boolean hasObstacle() {
        return hasObstacle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GraphNode)) return false;
        GraphNode that = (GraphNode) o;
        return Objects.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return position.hashCode();
    }



}
