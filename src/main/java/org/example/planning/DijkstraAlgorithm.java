package org.example.planning;

import org.example.agent.Position;

import java.util.*;

public class DijkstraAlgorithm {
    public static List<Position> computePath(Graph graph, Position start, Position goal) {
        // 🚀 Par défaut : éviter obstacle avec coût énorme
        return computePath(graph, start, goal, false);
    }

    public static List<Position> computePath(Graph graph, Position start, Position goal, boolean strictObstacleAvoidance) {
        GraphNode startNode = graph.getNode(start);
        GraphNode goalNode  = graph.getNode(goal);
        if (startNode == null || goalNode == null) {
            System.out.println("❌ Start/Goal introuvable");
            return Collections.emptyList();
        }

        Map<GraphNode, Double> dist   = new HashMap<>();
        Map<GraphNode, GraphNode> prev = new HashMap<>();
        Set<GraphNode> visited        = new HashSet<>();
        PriorityQueue<GraphNode> queue = new PriorityQueue<>(Comparator.comparingDouble(dist::get));

        for (GraphNode node : graph.getAllNodes()) {
            dist.put(node, Double.POSITIVE_INFINITY);
            prev.put(node, null);
        }
        dist.put(startNode, 0.0);
        queue.add(startNode);

        while (!queue.isEmpty()) {
            GraphNode current = queue.poll();
            if (!visited.add(current)) continue;

            if (current.equals(goalNode)) break;

            for (Map.Entry<GraphNode, Double> e : current.getNeighbors().entrySet()) {
                GraphNode neighbor = e.getKey();

                if (visited.contains(neighbor)) continue;

                double cost = e.getValue();

                if (neighbor.hasObstacle()) {
                    if (strictObstacleAvoidance) {
                        // 🚫 Strict : on refuse complètement de passer sur un obstacle
                        continue;
                    } else {
                        // 💥 Sinon, on pénalise seulement
                        cost += 1000.0;
                    }
                }

                double alt = dist.get(current) + cost;
                if (alt < dist.get(neighbor)) {
                    dist.put(neighbor, alt);
                    prev.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        List<Position> path = new ArrayList<>();
        for (GraphNode at = goalNode; at != null; at = prev.get(at)) {
            path.add(0, at.getPosition());
        }
        if (path.isEmpty() || !path.get(0).equals(start)) {
            System.out.println("⚠️ Chemin introuvable entre " + start + " et " + goal);
            return Collections.emptyList();
        }
        return path;
    }


}
