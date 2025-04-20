package org.example.planning;

import org.example.agent.Position;

import java.util.*;

public class Graph {
    private final Map<Position, GraphNode> nodes = new HashMap<>();

    public GraphNode getOrCreateNode(Position position) {
        return nodes.computeIfAbsent(position, GraphNode::new);
    }

    public void connect(Position a, Position b, double cost) {
        GraphNode nodeA = getOrCreateNode(a);
        GraphNode nodeB = getOrCreateNode(b);
        nodeA.addNeighbor(nodeB, cost);
        nodeB.addNeighbor(nodeA, cost); // bidirectional
    }

    public GraphNode getNode(Position position) {
        return nodes.get(position);
    }

    public Collection<GraphNode> getAllNodes() {
        return nodes.values();
    }
}
