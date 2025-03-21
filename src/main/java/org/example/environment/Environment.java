package org.example.environment;

import org.example.agent.Vehicle;

import java.util.ArrayList;
import java.util.List;


public class Environment {
    private int[][] grid;
    private List<Vehicle> vehicles;
    private List<TrafficLight> trafficLights;
    private static final double SAFE_DISTANCE = 10.0; // Distance de sécurité en unités

    public Environment(){
        this.vehicles = new ArrayList<>();
        this.trafficLights = new ArrayList<>();
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

    public void update(){
        // mettre à jour l'état de l'env
        for (Vehicle vehicle : vehicles){
            vehicle.perceivedEnvironment(this);
            vehicle.decideNextAction(this);
            vehicle.act();
        }
    }
}
