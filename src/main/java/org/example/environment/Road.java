package org.example.environment;

import org.example.agent.Vehicle;
import org.example.agent.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente une route dans l'environnement de simulation.
 * Peut contenir des véhicules, gérer leur positionnement et fournir des métriques.
 */
public class Road {
    private final String id;  // Identifiant unique de la route
    private final double length;  // Longueur de la route (en mètres ou en unités arbitraires)
    private static final int maxCapacity = 15;  // Capacité maximale de véhicules
    private final List<Position> entryPoints;  // Points d'entrée/sortie (intersections)
    private final List<TrafficLight> trafficLights;
    private final List<Lane> lanes;
    private boolean isCongested;  // Indique si la route est congestionnée


    // Constructeur
    public Road(String id, double length, int maxCapacity, List<Position> entryPoints) {
        this.id = id;
        this.length = length;
        this.entryPoints = entryPoints;
        this.isCongested = false;
        this.trafficLights = new ArrayList<>();
        this.lanes = new ArrayList<>();
    }

    public void addLane(Lane lane) {
        lanes.add(lane);
        lane.setRoad(this); // Si Lane a une référence à Road
    }
    public static int maxCapacityCount(){return maxCapacity;}
    public void addTrafficLight(TrafficLight trafficLight, Position position){
        trafficLights.add(trafficLight);
    }

    public List<TrafficLight> getTrafficLights() {
        return trafficLights;
    }

    public boolean isCongested() {
        return isCongested;
    }

    public String getId() {
        return id;
    }

    public double getLength() {
        return length;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public List<Position> getEntryPoints() {
        return entryPoints;
    }
}