package org.example;

import org.example.agent.Desire;
import org.example.agent.Vehicle;
import org.example.environment.Environment;
import org.example.environment.Obstacle;
import org.example.environment.Road;
import org.example.environment.TrafficLight;
import org.example.agent.Position;
import org.example.environment.Lane;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        // Créer l'environnement
        Environment env = new Environment();

        // Création d'une route de 100m de long avec une capacité de 10 véhicules
        List<Position> entryPoints = List.of(new Position(0, 0), new Position(100, 0));
        Road road = new Road("R1", 100.0, 10, entryPoints);

        // Créer une Lane (voie) associée à cette Road
        Lane lane = new Lane("L1", 3.5, 1.0, Lane.DIRECTION_RIGHT, road);
        Lane lane2 = new Lane("L2", 3.5, -1.0, Lane.DIRECTION_RIGHT, road);
        road.addLane(lane);
        road.addLane(lane2);

        // Ajouter un feu de circulation
        TrafficLight trafficLight = new TrafficLight("R1", "GREEN");
        road.addTrafficLight(trafficLight, new Position(30, 0));

        // Ajouter des véhicules à la Lane (au lieu de Road)
        Position destination = new Position(100, 0);
        Vehicle vehicle1 = new Vehicle(new Position(0, 1), destination);
        Vehicle vehicle2 = new Vehicle(new Position(15, 1), destination);
        Vehicle vehicle3 = new Vehicle(new Position(81, -1), destination);
        Vehicle vehicle4 = new Vehicle(new Position(92, -1), destination);
        lane.addVehicle(vehicle1);
        lane.addVehicle(vehicle2);
        lane2.addVehicle(vehicle3);
        lane2.addVehicle(vehicle4);

        // Ajouter un obstacle à la Lane
        Obstacle obstacle1 = new Obstacle(new Position(20, 1));
        Obstacle obstacle2 = new Obstacle(new Position(86, -1));
        lane.addObstacle(obstacle1);
        lane2.addObstacle(obstacle2);

        System.out.println(road);
        System.out.println("Congested? " + road.isCongested());  // false

        for (int i = 0; i < 20; i++) {
            System.out.println("=== Étape " + (i + 1) + " ===");

            // Alterner l'état des feux
            trafficLight.update();

            List<Vehicle> lane1Vehicles = new ArrayList<>(lane.getVehicles());
            List<Vehicle> lane2Vehicles = new ArrayList<>(lane2.getVehicles());

            // Mise à jour des véhicules
            for (Vehicle v : lane1Vehicles) {
                v.bdiCycle(lane, road);
            }
            for (Vehicle v : lane2Vehicles) {
                v.bdiCycle(lane2, road);
            }


            lane.removeArrivedVehicles();
            lane2.removeArrivedVehicles();


            displayLaneVehicles(lane, "L1");
            displayLaneVehicles(lane2, "L2");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    private static void printVehicleStatus(Vehicle vehicle, String name, Lane lane) {
        System.out.println(name + " (Lane " + lane.getId() + "):");
        System.out.println("  Position: " + vehicle.getPosition());
        System.out.println("  Dernière intention: " +
                (vehicle.getAllIntentions().isEmpty() ? "Aucune" :
                        vehicle.getAllIntentions().get(vehicle.getAllIntentions().size()-1)));
        System.out.println("  Croyances: " + vehicle.getBeliefs());
        System.out.println("  Désirs actifs: " +
                vehicle.getDesires().stream()
                        .filter(d -> !d.isAchieved())
                        .map(Desire::getName)
                        .collect(Collectors.toList()));
        System.out.println("------------------");
    }


    private static void displayLaneVehicles(Lane lane, String laneId) {
        if (!lane.getVehicles().isEmpty()) {
            System.out.println("\nLane " + laneId + ":");
            for (Vehicle vehicle : lane.getVehicles()) {
                printVehicleStatus(vehicle, "Véhicule " + vehicle.getId(), lane);
            }
        }
    }
}