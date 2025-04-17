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

        Environment env = new Environment();
        System.out.println("Environnement créé");

        List<Position> entryPoints = List.of(new Position(0, 0), new Position(100, 0));
        Road road = new Road("R1", 100.0, entryPoints);
        road.setCondition(Road.RoadCondition.WET);

        Lane lane = new Lane("L1", 3.5, 1.0, Lane.DIRECTION_RIGHT, road);
        Lane lane2 = new Lane("L2", 3.5, -1.0, Lane.DIRECTION_RIGHT, road);
        road.addLane(lane);
        road.addLane(lane2);

        TrafficLight trafficLight = new TrafficLight("R1", GREEN);
        road.addTrafficLight(trafficLight, new Position(100, 0));

        road.trainTrafficLights();              // 🔁 Calcule la politique MDP
        road.enableMDP(true);
        road.setMDPDecisionInterval(3);

        Obstacle barriere = new Obstacle(new Position(30, 1));
        Obstacle debris = new Obstacle(new Position(95, 1));
        Obstacle petion = new Obstacle(new Position(20, 1));
        Obstacle ralentisseur = new Obstacle(new Position(85, 1));
        Obstacle ralentisseur2 = new Obstacle(new Position(10, 1));

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

        allVehiclesEverCreated.addAll(List.of(
                vehicle1, vehicle2, vehicle3, vehicle4, vehicle5,
                vehicle6, vehicle7, vehicle8, vehicle9, vehicle10
        ));

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
        System.out.println("Congested? " + road.isCongested());

        // 🔁 Simulation prolongée : 40 étapes
        for (int i = 0; i < 40; i++) {
            System.out.println("=== Étape " + (i + 1) + " ===");

            road.updateTrafficConditions();     // met à jour trafic + politique
            road.updateTrafficLights();         // applique les décisions MDP

            // 🔎 LOG : décision du feu
            String state = trafficLight.getCurrentState();
            String action = trafficLight.getPolicy().getOrDefault(state, "AUCUNE");
            System.out.println("🟢 Feu (" + state + ") → Action: " + action);
            System.out.println("🎨 Couleur actuelle du feu: " + trafficLight.getState());

            // 💥 À mi-parcours, injecter plus de trafic
            if (i == 20) {
                System.out.println("⚠️ Phase 2 : On injecte du trafic !");
                for (int j = 0; j < 10; j++) {
                    Vehicle v = new Vehicle(new Position(5 + j * 2, 1), destination);
                    lane.addVehicle(v);
                    allVehiclesEverCreated.add(v);
                }
            }

            // 🔄 Mise à jour des véhicules
            for (Vehicle v : new ArrayList<>(lane.getVehicles())) {
                v.bdiCycle(lane, road);
            }
            for (Vehicle v : new ArrayList<>(lane2.getVehicles())) {
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

        // 📊 Affichage des métriques de performance
        displayMetrics(allVehiclesEverCreated);
    }

    private static void printVehicleStatus(Vehicle vehicle, String name, Lane lane) {
        System.out.println(name + " (Lane " + lane.getId() + "):");
        System.out.println("  Position: " + vehicle.getPosition());
        System.out.println("  Dernière intention: " +
                (vehicle.getAllIntentions().isEmpty() ? "Aucune" :
                        vehicle.getAllIntentions().get(vehicle.getAllIntentions().size() - 1)));
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

        System.out.println("\nRésultats globaux:");
        System.out.printf("- Temps moyen de trajet: %.2f secondes%n", avgTime);
        System.out.printf("- Nombre moyen de changements de voie: %.2f%n", avgLaneChanges);
        System.out.printf("- Niveau moyen de frustration: %.2f%n", avgFrustration);
        System.out.printf("- Véhicules arrivés à destination: %d/%d%n", arrivedVehicles, allVehicles.size());

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
