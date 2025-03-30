package org.example.environment;
/*
The Lane class models a traffic lane in a simulated road network. It acts as an interface between the vehicles
(Vehicle) and the environment (Road), managing :
the physical position of vehicles and obstacles
Local interactions (collision detection, traffic lights)
Traffic logic (direction of travel, maximum capacity)
 */
import org.example.agent.Position;
import org.example.agent.Vehicle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Lane {
    private final String id;
    private final double width;
    private final List<Vehicle> vehicles;
    private final List<Obstacle> obstacles;
    private final double SAFE_DISTANCE = 10.0;
    private Road road;
    private final double centerY;
    private final int direction;
    public static final int DIRECTION_RIGHT = 0;   // Sens croissant X
    public static final int DIRECTION_LEFT = 180;  // Sens décroissant X

    public Lane(String id, double width, double centerY,int direction, Road road) {
        this.id = id;
        this.width = width;
        this.centerY = centerY;
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

    public boolean isCarAhead(Vehicle vehicle) {
        Position currentPos = vehicle.getPosition();
        boolean isReverseLane = direction == 180;

        for (Vehicle other : vehicles) {
            if (!other.equals(vehicle)) {
                Position otherPos = other.getPosition();
                if (Math.abs(otherPos.getY() - currentPos.getY()) < 2) {
                    double distance = isReverseLane
                            ? currentPos.getX() - otherPos.getX()
                            : otherPos.getX() - currentPos.getX();

                    if (distance > 0 && distance < SAFE_DISTANCE) {
                        System.out.println("Véhicule détecté devant à " + distance + " unités");
                        return true;
                    }
                }
            }
        }
        System.out.println("Aucun véhicule détecté devant.");
        return false;
    }

    public Vehicle getVehicleAhead(Vehicle vehicle) {
        Position currentPos = vehicle.getPosition();
        double closestDistance = Double.MAX_VALUE;
        Vehicle closestVehicle = null;

        for (Vehicle other : vehicles) {
            if (!other.equals(vehicle)) {
                Position otherPos = other.getPosition();
                if (Math.abs(otherPos.getY() - currentPos.getY()) < 2) {
                    double distance = (direction == DIRECTION_LEFT)
                            ? currentPos.getX() - otherPos.getX()
                            : otherPos.getX() - currentPos.getX();

                    if (distance > 0 && distance < closestDistance) {
                        closestDistance = distance;
                        closestVehicle = other;
                    }
                }
            }
        }
        return closestVehicle;
    }


    public boolean isCarOnLeft(Vehicle vehicle) {
        Position currentPos = vehicle.getPosition();
        for (Vehicle other : vehicles) {
            if (!other.equals(vehicle)) {
                Position otherPos = other.getPosition();
                // Vérifie à gauche (X inférieur et même Y)
                if (Math.abs(otherPos.getY() - currentPos.getY()) < 2 &&
                        otherPos.getX() < currentPos.getX()) {
                    double distance = currentPos.distanceTo(otherPos);
                    if (distance < SAFE_DISTANCE) {
                        System.out.println("Véhicule détecté à gauche : " + other);
                        return true;
                    }
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
    public boolean removeVehicle(Vehicle vehicle) {
        return vehicles.remove(vehicle);
    }
    public void removeArrivedVehicles() {
        Iterator<Vehicle> iterator = vehicles.iterator();
        while (iterator.hasNext()) {
            Vehicle vehicle = iterator.next();
            if (vehicle.getBeliefs().contains("AtDestination", true)) {
                iterator.remove();
                System.out.println("Véhicule " + vehicle + " a atteint sa destination et a été retiré");
            }
        }
    }


    public boolean isObstacleAhead(Vehicle vehicle) {
        Position vehiclePos = vehicle.getPosition();
        for (Obstacle obstacle : obstacles) {
            Position obsPos = obstacle.getPosition();
            if (Math.abs(obsPos.getY() - vehiclePos.getY()) < 2) {
                double distance = obsPos.getX() - vehiclePos.getX(); // Supprimer le traitement de direction
                if (distance > 0 && distance < SAFE_DISTANCE) {
                    System.out.println("Obstacle détecté à " + distance + "m");
                    return true;
                }
            }
        }
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

    public boolean isInTrafficJam() {
        return getVehicleCount() > getRoad().getMaxCapacity() * 0.7;
    }

    public boolean isPriorityVehicleNearby(Vehicle vehicle) {
        return false;
    }
    //gets

    public int getVehicleCount() {
        return vehicles.size();
    }
    public Road getRoad() {
        return this.road;
    }
    public List<Vehicle> getVehicles() {
        return this.vehicles; // Retourne la liste directe (attention à la modification externe)
    }
    public int getDirection() {
        return direction;
    }
    public boolean isSameDirection(Lane other) {
        return this.direction == other.getDirection();
    }
    public String getId() {
        return this.id;
    }
    public double getCenterY() {
        return centerY;
    }
    public int getCenterYInt() {
        return (int) Math.round(this.centerY);
    }

    //next implementations
    public double getVehicleSpeed(Vehicle vehicle) {
        // Vitesse de base réduite si feu rouge ou obstacle
        if (!isTrafficLightGreen(road, road.getId())) {
            return 0.0;
        }
        if (isObstacleAhead(vehicle)) {
            return 20.0;
        }
        return 50.0; // Vitesse normale
    }
    public double getSafeDistance() {
        return SAFE_DISTANCE;
    }
    public void updateAllVehicles(Road road) {
        for (Vehicle vehicle : vehicles) {
            vehicle.bdiCycle(this, road);
        }
    }

}
