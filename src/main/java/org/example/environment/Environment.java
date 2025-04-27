package org.example.environment;

import org.example.agent.Position;
import org.example.agent.Vehicle;
import org.example.planning.Graph;
import org.example.planning.GraphNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
the basic environment, which will then include roads, which in turn will include vehicles, etc. etc.

NOTE: we'll use grid later to create a 2d interface for the moment we're content with the text output,
giving priority to the functional over the superficial.
 */

public class Environment {
    private Graph globalGraph = new Graph();
    private List<Road> roads;
    //private List<In> intersections;
    public Environment(){
        this.roads = new ArrayList<>();
    }

    public void buildGlobalGraph() {
        globalGraph = new Graph();
        int segmentLength = 10;

        for (Road road : roads) {
            road.initGraphForPathfinding(); // chaque route initialise son graphe local
            Graph localGraph = road.getGraph();

            // Intégrer les noeuds dans le graphe global
            for (GraphNode node : localGraph.getAllNodes()) {
                globalGraph.getOrCreateNode(node.getPosition());
            }

            // Intégrer les connexions (bonds locaux)
            for (GraphNode node : localGraph.getAllNodes()) {
                GraphNode globalNode = globalGraph.getNode(node.getPosition());
                for (Map.Entry<GraphNode, Double> entry : node.getNeighbors().entrySet()) {
                    globalGraph.connect(globalNode.getPosition(), entry.getKey().getPosition(), entry.getValue());
                }
            }
        }

        // 🔁 Connexion entre routes via leurs entryPoints
        for (int i = 0; i < roads.size(); i++) {
            Road roadA = roads.get(i);
            for (int j = i + 1; j < roads.size(); j++) {
                Road roadB = roads.get(j);

                for (Position endA : roadA.getEntryPoints()) {
                    for (Position startB : roadB.getEntryPoints()) {
                        if (endA.distanceTo(startB) < 15.0) {
                            globalGraph.connect(endA.snapToGrid(segmentLength), startB.snapToGrid(segmentLength), 5);
                        }
                    }
                }
            }
        }

        System.out.println("🌐 Graphe GLOBAL construit avec " + globalGraph.getAllNodes().size() + " nœuds.");
    }

    public Graph getGlobalGraph() {
        return globalGraph;
    }
    public List<Road> getRoads() {
        return roads;
    }


}
