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

import static org.example.environment.TrafficLight.LightColor.GREEN;

/*
ALL simulations implementations, car, obstacles etc
Will be updated for the next part with better interface and more complexes scenarios
 */

public class Main {
    private static List<Vehicle> allVehiclesEverCreated = new ArrayList<>();
    public static void main(String[] args) {
        System.out.println("Démarrage de la simulation...");
        // Créer l'environnement
        Environment env = new Environment();
        System.out.println("Environnement créé"); // <-- Log de progression
        // Création d'une route de 100m de long avec une capacité de 10 véhicules
        List<Position> entryPoints = List.of(new Position(0, 0), new Position(100, 0));
        Road road = new Road("R1", 100.0, entryPoints);

        road.setCondition(Road.RoadCondition.WET);

        // Créer une Lane (voie) associée à cette Road
        Lane lane = new Lane("L1", 3.5, 1.0, Lane.DIRECTION_RIGHT, road);
        Lane lane2 = new Lane("L2", 3.5, -1.0, Lane.DIRECTION_RIGHT, road);
        road.addLane(lane);
        road.addLane(lane2);

        // Ajouter un feu de circulation
        TrafficLight trafficLight = new TrafficLight("R1", GREEN);
        road.addTrafficLight(trafficLight, new Position(100, 0));

        road.trainTrafficLights();// Train the MDP policies
        road.enableMDP(true); // Enable MDP control
        road.setMDPDecisionInterval(3); // Make decisions every 5 steps

        Obstacle barriere = new Obstacle(new Position(30,1));
        Obstacle debris = new Obstacle(new Position(95,1));
        Obstacle petion = new Obstacle(new Position(20,1));
        Obstacle ralentisseur = new Obstacle(new Position(85,1));
        Obstacle ralentisseur2 = new Obstacle(new Position(10,1));

        // Ajouter des véhicules à la Lane (au lieu de Road)
        Position destination = new Position(100, 1);
        Position destination2 = new Position(25, 1);
        Vehicle vehicle1 = new Vehicle(new Position(0, 1), destination);
        Vehicle vehicle2 = new Vehicle(new Position(15, 1), destination2);
        Vehicle vehicle3 = new Vehicle(new Position(81, -1), destination);
        Vehicle vehicle4 = new Vehicle(new Position(92, -1), destination);
        Vehicle vehicle5 = new Vehicle(new Position(0, 1), destination);
        Vehicle vehicle6 = new Vehicle(new Position(15, 1), destination2);
        Vehicle vehicle7 = new Vehicle(new Position(81, -1), destination);
        Vehicle vehicle8 = new Vehicle(new Position(92, -1), destination);
        Vehicle vehicle9 = new Vehicle(new Position(81, -1), destination);
        Vehicle vehicle10 = new Vehicle(new Position(92, -1), destination);


        allVehiclesEverCreated.add(vehicle1);
        allVehiclesEverCreated.add(vehicle2);
        allVehiclesEverCreated.add(vehicle3);
        allVehiclesEverCreated.add(vehicle4);
        allVehiclesEverCreated.add(vehicle5);
        allVehiclesEverCreated.add(vehicle6);
        allVehiclesEverCreated.add(vehicle7);
        allVehiclesEverCreated.add(vehicle8);
        allVehiclesEverCreated.add(vehicle9);
        allVehiclesEverCreated.add(vehicle10);


        lane.addVehicle(vehicle1);
        lane.addVehicle(vehicle2);
        lane.addVehicle(vehicle3);
        lane.addVehicle(vehicle4);
        lane.addVehicle(vehicle5);
        lane2.addVehicle(vehicle6);
        lane2.addVehicle(vehicle7);


        lane.addObstacle(barriere);
        lane2.addObstacle(debris);
        lane.addObstacle(petion);
        lane2.addObstacle(ralentisseur);
        lane.addObstacle(ralentisseur2);

        System.out.println(road);
        System.out.println("Congested? " + road.isCongested());  // false

        for (int i = 0; i < 20; i++) {
            System.out.println("=== Étape " + (i + 1) + " ===");

            road.updateTrafficConditions();

            // Alterner l'état des feux
            road.updateTrafficLights();

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

        // Calcul et affichage des métriques après la simulation
        displayMetrics(allVehiclesEverCreated);
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

    private static void displayMetrics(List<Vehicle> allVehicles) {
        System.out.println("\n=== Métriques de simulation ===");

        // Calcul des moyennes
        double totalTime = 0;
        int totalLaneChanges = 0;
        int totalFrustration = 0;
        int arrivedVehicles = 0;

        for (Vehicle vehicle : allVehicles) {
            totalTime += vehicle.getTravelTimeSeconds();
            totalLaneChanges += vehicle.getLaneChangeCount();
            totalFrustration += vehicle.getFrustrationCount();

            if (vehicle.getBeliefs().contains("AtDestination", true)) {
                arrivedVehicles++;
            }
        }

        double avgTime = allVehicles.isEmpty() ? 0 : totalTime / allVehicles.size();
        double avgLaneChanges = allVehicles.isEmpty() ? 0 : (double) totalLaneChanges / allVehicles.size();
        double avgFrustration = allVehicles.isEmpty() ? 0 : (double) totalFrustration / allVehicles.size();

        // Affichage des résultats globaux
        System.out.println("\nRésultats globaux:");
        System.out.printf("- Temps moyen de trajet: %.2f secondes%n", avgTime);
        System.out.printf("- Nombre moyen de changements de voie: %.2f%n", avgLaneChanges);
        System.out.printf("- Niveau moyen de frustration: %.2f%n", avgFrustration);
        System.out.printf("- Véhicules arrivés à destination: %d/%d%n", arrivedVehicles, allVehicles.size());

        // Détails par véhicule
        System.out.println("\nDétails par véhicule:");
        for (Vehicle vehicle : allVehicles) {
            System.out.printf("Véhicule %d - Temps: %.1fs, Changements: %d, Frustration: %d, Arrivé: %s%n",
                    vehicle.getId(),
                    vehicle.getTravelTimeSeconds(),
                    vehicle.getLaneChangeCount(),
                    vehicle.getFrustrationCount(),
                    vehicle.getBeliefs().contains("AtDestination", true) ? "Oui" : "Non");
        }
    }
}