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
        lane.setRoad(this);
    }
    public static int maxCapacityCount(){return maxCapacity;}
    public void addTrafficLight(TrafficLight trafficLight, Position position){
        trafficLights.add(trafficLight);
    }

    public List<TrafficLight> getTrafficLights() {
        return trafficLights;
    }
    public boolean hasLeftLane(Lane currentLane) {
        int index = lanes.indexOf(currentLane);
        return index > 0;
    }
    public boolean hasRightLane(Lane currentLane) {
        int index = lanes.indexOf(currentLane);
        return index < lanes.size() - 1;
    }
    public Lane getLeftLane(Lane currentLane) {
        if (!hasLeftLane(currentLane)) return null;
        return lanes.get(lanes.indexOf(currentLane) - 1);
    }
    public Lane getRightLane(Lane currentLane) {
        if (!hasRightLane(currentLane)) return null;
        return lanes.get(lanes.indexOf(currentLane) + 1);
    }
    public boolean isCongested() {
        return isCongested;
    }

    public String getId() {
        return id;
    }


    public int getMaxCapacity() {
        return maxCapacity;
    }

}