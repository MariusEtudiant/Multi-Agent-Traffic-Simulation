package org.example.environment;

import org.example.agent.Position;
import org.example.agent.Vehicle;

import java.util.ArrayList;
import java.util.List;

public class Lane {
    private final String id;
    private final double width;
    private final int direction;
    private final List<Vehicle> vehicles;
    private final List<Obstacle> obstacles;
    private final double SAFE_DISTANCE = 10.0;
    private Road road;

    public Lane(String id, double width, int direction, Road road) {
        this.id = id;
        this.width = width;
        this.direction = direction;
        this.vehicles = new ArrayList<>();
        this.obstacles = new ArrayList<>();
        this.road = road;
    }
    public void setRoad(Road road) {
        this.road = road;
    }

    public void addVehicle(Vehicle vehicle) {
        if (vehicles.size() >= Road.maxCapacityCount()) {
            throw new IllegalStateException("Road " + id + " is at full capacity!");
        }
        vehicles.add(vehicle);
    }
    public void addObstacle(Obstacle obstacle){obstacles.add(obstacle);}
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

    public boolean isTrafficLightGreen(Road road, String routeId) {
        List<TrafficLight> lights = road.getTrafficLights();
        for (TrafficLight trafficLight : lights) {
            if (trafficLight.getId().equals(routeId)) {
                System.out.println("Feu de circulation " + routeId + " est " + trafficLight.getState());
                return trafficLight.getState().equals("GREEN");
            }
        }
        System.out.println("Feu de circulation " + routeId + " non trouvé.");
        return false;
    }

    public boolean containsVehicle(Vehicle vehicle) {
        return vehicles.contains(vehicle);
    }

    public int getVehicleCount() {
        return vehicles.size();
    }
    public Road getRoad() {
        return this.road;
    }

    public void update(){
        // mettre à jour l'état de l'env
        for (Vehicle vehicle : vehicles){
            vehicle.perceivedEnvironment(this, this.getRoad());
            vehicle.decideNextAction(this);
            vehicle.act();
        }
    }
}
