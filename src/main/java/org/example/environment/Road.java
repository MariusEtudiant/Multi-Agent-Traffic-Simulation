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
    private final int maxCapacity;  // Capacité maximale de véhicules
    private final List<Vehicle> vehicles;  // Liste des véhicules sur la route
    private final List<Position> entryPoints;  // Points d'entrée/sortie (intersections)
    private final List<Obstacle> obstacles;
    private final List<TrafficLight> trafficLights;
    private boolean isCongested;  // Indique si la route est congestionnée
    private static final double SAFE_DISTANCE = 10.0; // Distance de sécurité en unités


    // Constructeur
    public Road(String id, double length, int maxCapacity, List<Position> entryPoints) {
        this.id = id;
        this.length = length;
        this.maxCapacity = maxCapacity;
        this.vehicles = new ArrayList<>();
        this.entryPoints = entryPoints;
        this.isCongested = false;
        this.obstacles = new ArrayList<>();
        this.trafficLights = new ArrayList<>();
    }

    // === Méthodes de gestion des véhicules ===

    /**
     * Ajoute un véhicule à la route.
     * @param vehicle Le véhicule à ajouter.
     * @throws IllegalStateException Si la route est pleine.
     */
    public void addVehicle(Vehicle vehicle) {
        if (vehicles.size() >= maxCapacity) {
            throw new IllegalStateException("Road " + id + " is at full capacity!");
        }
        vehicles.add(vehicle);
        updateCongestionStatus();
    }
    public void addTrafficLight(TrafficLight trafficLight, Position position){
        trafficLights.add(trafficLight);
    }
    public void addObstacle(Obstacle obstacle){obstacles.add(obstacle);}

    public boolean isTrafficLightGreen(String routeId) {
        for (TrafficLight trafficLight : trafficLights) {
            if (trafficLight.getId().equals(routeId)) {
                System.out.println("Feu de circulation " + routeId + " est " + trafficLight.getState());
                return trafficLight.getState().equals("GREEN");
            }
        }
        System.out.println("Feu de circulation " + routeId + " non trouvé.");
        return false;
    }
    public List<TrafficLight> getTrafficLights() {
        return trafficLights;
    }
    public double getSafeDistance() {
        return SAFE_DISTANCE;
    }

    public boolean isCarAhead(Vehicle vehicle) {
        for (Vehicle other : vehicles) {
            if (!other.equals(vehicle)) {
                double distance = vehicle.getPosition().distanceTo(other.getPosition());
                if (distance < SAFE_DISTANCE) {
                    System.out.println("Véhicule détecté devant : " + other + " à une distance de " + distance);
                    return true;
                }
            }
        }
        System.out.println("Aucun véhicule détecté devant.");
        return false;
    }

    public boolean isCarBehind(Vehicle vehicle) {
        for (Vehicle other : vehicles) {
            if (!other.equals(vehicle)) {
                double distance = vehicle.getPosition().distanceTo(other.getPosition());
                if (distance < SAFE_DISTANCE && other.getPosition().getY() < vehicle.getPosition().getY()) {
                    System.out.println("Véhicule détecté derrière : " + other + " à une distance de " + distance);
                    return true;
                }
            }
        }
        System.out.println("Aucun véhicule détecté derrière.");
        return false;
    }

    public boolean isCarOnLeft(Vehicle vehicle) {
        for (Vehicle other : vehicles) {
            if (!other.equals(vehicle)) {
                double distance = vehicle.getPosition().distanceTo(other.getPosition());
                if (distance < SAFE_DISTANCE && other.getPosition().getX() < vehicle.getPosition().getX()) {
                    System.out.println("Véhicule détecté à gauche : " + other + " à une distance de " + distance);
                    return true;
                }
            }
        }
        System.out.println("Aucun véhicule détecté à gauche.");
        return false;
    }
    public boolean isCarOnRight(Vehicle vehicle) {
        for (Vehicle other : vehicles) {
            if (!other.equals(vehicle)) {
                double distance = vehicle.getPosition().distanceTo(other.getPosition());
                if (distance < SAFE_DISTANCE && other.getPosition().getX() > vehicle.getPosition().getX()) {
                    System.out.println("Véhicule détecté à droite : " + other + " à une distance de " + distance);
                    return true;
                }
            }
        }
        System.out.println("Aucun véhicule détecté à droite.");
        return false;
    }

    public boolean isObstacleAhead(Vehicle vehicle) {
        // Supposons que vous avez une liste d'obstacles dans l'environnement
        for (Obstacle obstacle : obstacles) {
            double distance = vehicle.getPosition().distanceTo(obstacle.getPosition());
            if (distance < SAFE_DISTANCE) {
                System.out.println("Obstacle détecté devant : " + obstacle + " à une distance de " + distance);
                return true;
            }
        }
        System.out.println("Aucun obstacle détecté devant.");
        return false;
    }

    public void update(){
        // mettre à jour l'état de l'env
        for (Vehicle vehicle : vehicles){
            vehicle.perceivedEnvironment(this);
            vehicle.decideNextAction(this);
            vehicle.act();
        }
    }
    /**
     * Vérifie si un véhicule est présent sur la route.
     */
    public boolean containsVehicle(Vehicle vehicle) {
        return vehicles.contains(vehicle);
    }

    /**
     * Retourne le nombre de véhicules sur la route.
     */
    public int getVehicleCount() {
        return vehicles.size();
    }

    // === Méthodes de congestion ===

    /**
     * Met à jour l'état de congestion (seuil arbitraire : 80% de capacité).
     */
    private void updateCongestionStatus() {
        isCongested = vehicles.size() >= 0.8 * maxCapacity;
    }

    public boolean isCongested() {
        return isCongested;
    }


    /**
     * Trouve le véhicule le plus proche d'une position donnée.
     */
    public Vehicle getClosestVehicle(Position position) {
        // Implémentation simplifiée (à améliorer avec une vraie logique de distance)
        return vehicles.isEmpty() ? null : vehicles.get(0);
    }

    // === Getters ===
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

    public List<Vehicle> getVehicles() {
        return new ArrayList<>(vehicles);  // Retourne une copie pour éviter les modifications externes
    }

    @Override
    public String toString() {
        return "Road " + id + " (Length: " + length + ", Vehicles: " + vehicles.size() + "/" + maxCapacity + ")";
    }
}