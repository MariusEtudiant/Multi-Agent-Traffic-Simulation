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

    public boolean isTrafficLightGreen(String intersectionId) {
        return trafficLights.stream()
                .filter(tl -> tl.getId().equals(intersectionId))
                .anyMatch(tl -> tl.getState().equals("GREEN"));
    }

    public boolean isCarAhead(Vehicle vehicle) {
        for (Vehicle other : vehicles) {
            if (!other.equals(vehicle)) {
                double distance = vehicle.getPosition().distanceTo(other.getPosition());
                if (distance < SAFE_DISTANCE) {
                    return true;
                }
            }
        }
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
