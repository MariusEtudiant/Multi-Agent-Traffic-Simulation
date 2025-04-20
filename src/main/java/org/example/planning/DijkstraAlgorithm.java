package org.example.planning;

import org.example.agent.Position;

import java.util.*;

public class DijkstraAlgorithm {
    public static List<Position> computePath(Graph graph, Position start, Position goal) {
        GraphNode startNode = graph.getNode(start);
        GraphNode goalNode  = graph.getNode(goal);
        if (startNode == null || goalNode == null) {
            System.out.println("❌ No start or goal node found in the graph.");
            return Collections.emptyList();
        }

        Map<GraphNode, Double> dist   = new HashMap<>();
        Map<GraphNode, GraphNode> prev = new HashMap<>();
        Set<GraphNode> visited        = new HashSet<>();
        // File de priorité basée sur dist; on réinsère à chaque relaxation
        PriorityQueue<GraphNode> queue = new PriorityQueue<>(Comparator.comparingDouble(dist::get));

        // Initialisation
        for (GraphNode node : graph.getAllNodes()) {
            dist.put(node, Double.POSITIVE_INFINITY);
            prev.put(node, null);
        }
        dist.put(startNode, 0.0);
        queue.add(startNode);

        // Boucle principale
        while (!queue.isEmpty()) {
            GraphNode current = queue.poll();
            if (!visited.add(current)) {
                // déjà traité → on ignore cette entrée (entrée obsolète ou doublon)
                continue;
            }
            if (current.equals(goalNode)) {
                // On a trouvé le but, on peut sortir
                break;
            }
            // Relaxation des arêtes sortantes
            for (Map.Entry<GraphNode, Double> e : current.getNeighbors().entrySet()) {
                GraphNode neighbor = e.getKey();
                if (visited.contains(neighbor)) {
                    continue;
                }
                double alt = dist.get(current) + e.getValue();
                if (alt < dist.get(neighbor)) {
                    dist.put(neighbor, alt);
                    prev.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        // Reconstruction du chemin
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
