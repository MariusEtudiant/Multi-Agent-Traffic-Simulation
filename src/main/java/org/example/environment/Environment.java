package org.example.environment;

import org.example.agent.Vehicle;

import java.util.ArrayList;
import java.util.List;


public class Environment {
    private int[][] grid;
    private List<Vehicle> vehicles;
    private List<TrafficLight> trafficLights;
    private List<Obstacle> obstacles;
    private static final double SAFE_DISTANCE = 10.0; // Distance de sécurité en unités

    public Environment(){
        this.vehicles = new ArrayList<>();
        this.trafficLights = new ArrayList<>();
        this.obstacles = new ArrayList<>();
    }
    public List<Vehicle> getVehicles() {
        return vehicles;
    }
    public List<TrafficLight> getTrafficLights() {
        return trafficLights;
    }
    public void addVehicle(Vehicle vehicle){
        vehicles.add(vehicle);
    }
    public void addTrafficLight(TrafficLight trafficLight){
        trafficLights.add(trafficLight);
    }
    public void addObstacle(Obstacle obstacle){obstacles.add(obstacle);}
    public double getSafeDistance() {
        return SAFE_DISTANCE;
    }

    public boolean isTrafficLightGreen(String intersectionId) {
        for (TrafficLight trafficLight : trafficLights) {
            if (trafficLight.getId().equals(intersectionId)) {
                System.out.println("Feu de circulation " + intersectionId + " est " + trafficLight.getState());
                return trafficLight.getState().equals("GREEN");
            }
        }
        System.out.println("Feu de circulation " + intersectionId + " non trouvé.");
        return false;
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
}
