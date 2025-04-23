package org.example;

import org.example.ArgumentationDM.DungGraphPanel;
import org.example.ArgumentationDM.TransportationAgent;
import org.example.agent.Vehicle;
import org.example.environment.Environment;
import org.example.environment.Obstacle;
import org.example.environment.Road;
import org.example.environment.TrafficLight;
import org.example.agent.Position;
import org.example.environment.Lane;
import org.tweetyproject.arg.dung.reasoner.SimpleGroundedReasoner;
import org.tweetyproject.arg.dung.semantics.Extension;
import org.tweetyproject.arg.dung.syntax.DungTheory;

import javax.swing.*;
import javax.swing.Timer;
import java.util.*;

import static org.example.environment.TrafficLight.LightColor.GREEN;

public class Main {
    public static void main(String[] args) {
        System.out.println("Démarrage de la simulation...");

        // Lancer un des scénarios ici :
        //runScenario1();
        //runScenario2();
        // runScenario3();
        runScenario4();
        runBatchSimulation();
    }

    /**
     * Scénario 1 : Une route simple à deux voies, un feu, quelques véhicules
     */
    public static void runScenario1() {
        System.out.println("\n=== SCÉNARIO 1 : Route simple à deux voies + feu ===");

        Environment env = new Environment();
        Road road = new Road("R1", 100.0, List.of(new Position(0, 0)));
        Lane lane1 = new Lane("L1", 3.5, 1.0, Lane.DIRECTION_RIGHT, road);
        Lane lane2 = new Lane("L2", 3.5, -1.0, Lane.DIRECTION_RIGHT, road);
        road.addLane(lane1);
        road.addLane(lane2);

        TrafficLight light = new TrafficLight("R1", GREEN);
        road.addTrafficLight(light, new Position(80, 1));
        road.trainTrafficLights();
        road.enableMDP(true);
        road.setMDPDecisionInterval(3);

        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle v1 = new Vehicle(new Position(0, 1), new Position(100, 1), env);
        Vehicle v2 = new Vehicle(new Position(10, 1), new Position(100, 1), env);
        lane1.addVehicle(v1);
        lane1.addVehicle(v2);
        vehicles.addAll(List.of(v1, v2));

        env.getRoads().add(road);
        env.buildGlobalGraph();

        for (int i = 0; i < 30; i++) {
            System.out.printf("\n⏱️ Étape %02d\n", i + 1);

            road.updateTrafficConditions();
            road.updateTrafficLights();

            System.out.printf("🔦 Feu (%s) → Action: %s\n",
                    light.getCurrentState(),
                    light.getPolicy().getOrDefault(light.getCurrentState(), "AUCUNE"));

            for (Vehicle v : new ArrayList<>(lane1.getVehicles())) {
                v.bdiCycle(lane1, road);
            }
            lane1.removeArrivedVehicles();
            displayLaneVehicles(lane1);

            if (i == 15) {
                System.out.println("⚠️ [Phase 2] Injection de trafic !");
                for (int j = 0; j < 3; j++) {
                    Vehicle extra = new Vehicle(new Position(5 + j * 5, 1), new Position(100, 1), env);
                    lane1.addVehicle(extra);
                    vehicles.add(extra);
                }
            }

            sleep(500);
        }

        displayMetrics(vehicles);
    }

    /**
     * Scénario 2 : Deux routes perpendiculaires avec feux de croisement
     */
    public static void runScenario2() {
        System.out.println("\n=== SCÉNARIO 2 : Intersections & feux croisés ===");

        Environment env = new Environment();

        // Route horizontale
        Road horizontal = new Road("H1", 100.0, List.of(new Position(0, 0)));
        Lane hLane1 = new Lane("HL1", 3.5, 1.0, Lane.DIRECTION_RIGHT, horizontal);
        Lane hLane2 = new Lane("HL2", 3.5, -1.0, Lane.DIRECTION_RIGHT, horizontal);
        horizontal.addLane(hLane1);
        horizontal.addLane(hLane2);

        // Route verticale
        Road vertical = new Road("V1", 100.0, List.of(new Position(50, -50)));
        Lane vLane1 = new Lane("VL1", 3.5, 51.0, Lane.DIRECTION_RIGHT, vertical);
        Lane vLane2 = new Lane("VL2", 3.5, 49.0, Lane.DIRECTION_RIGHT, vertical);
        vertical.addLane(vLane1);
        vertical.addLane(vLane2);

        // Feu au croisement
        TrafficLight crossLight = new TrafficLight("H1", GREEN);
        TrafficLight crossLight2 = new TrafficLight("V1", GREEN);
        horizontal.addTrafficLight(crossLight, new Position(50, 1));
        vertical.addTrafficLight(crossLight2, new Position(50, 1));
        horizontal.trainTrafficLights();
        horizontal.enableMDP(true);

        // Véhicules
        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle vh1 = new Vehicle(new Position(0, 1), new Position(50, -50), env);
        Vehicle vv1 = new Vehicle(new Position(50, 51), new Position(50, -50), env);
        hLane1.addVehicle(vh1);
        vLane1.addVehicle(vv1);
        vehicles.addAll(List.of(vh1, vv1));

        env.getRoads().add(horizontal);
        env.getRoads().add(vertical);
        env.buildGlobalGraph();

        for (int i = 0; i < 30; i++) {
            System.out.printf("\n⏱️ Étape %02d\n", i + 1);
            horizontal.updateTrafficConditions();
            horizontal.updateTrafficLights();

            System.out.printf("🔦 Feu (%s) → Action: %s\n",
                    crossLight.getCurrentState(),
                    crossLight.getPolicy().getOrDefault(crossLight.getCurrentState(), "AUCUNE"));

            for (Vehicle v : new ArrayList<>(hLane1.getVehicles())) {
                v.bdiCycle(hLane1, horizontal);
            }
            for (Vehicle v : new ArrayList<>(vLane1.getVehicles())) {
                v.bdiCycle(vLane1, vertical);
            }

            hLane1.removeArrivedVehicles();
            vLane1.removeArrivedVehicles();

            displayLaneVehicles(hLane1);
            displayLaneVehicles(vLane1);

            sleep(500);
        }

        displayMetrics(vehicles);
    }

    /**
     * Scénario 3 : Route complexe avec obstacles et embouteillages
     */
    public static void runScenario3() {
        System.out.println("\n=== SCÉNARIO 3 : Obstacles & trafic dense ===");

        Environment env = new Environment();
        Road road = new Road("R1", 100.0, List.of(new Position(0, 0)));
        Lane lane1 = new Lane("L1", 3.5, 1.0, Lane.DIRECTION_RIGHT, road);
        Lane lane2 = new Lane("L2", 3.5, -1.0, Lane.DIRECTION_RIGHT, road);
        road.addLane(lane1);
        road.addLane(lane2);

        TrafficLight trafficLight = new TrafficLight("R1", GREEN);
        road.addTrafficLight(trafficLight, new Position(90, 1));
        road.trainTrafficLights();
        road.enableMDP(true);
        road.setMDPDecisionInterval(3);

        // Obstacles
        lane1.addObstacle(new Obstacle(new Position(30, 1)));
        lane1.addObstacle(new Obstacle(new Position(45, 1)));
        lane2.addObstacle(new Obstacle(new Position(55, -1)));

        List<Vehicle> vehicles = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Vehicle v = new Vehicle(new Position(i * 5, 1), new Position(100, 1), env);
            lane1.addVehicle(v);
            vehicles.add(v);
        }

        env.getRoads().add(road);
        env.buildGlobalGraph();

        for (int i = 0; i < 40; i++) {
            System.out.printf("\n⏱️ Étape %02d\n", i + 1);
            road.updateTrafficConditions();
            road.updateTrafficLights();

            System.out.printf("🔦 Feu (%s) → Action: %s\n",
                    trafficLight.getCurrentState(),
                    trafficLight.getPolicy().getOrDefault(trafficLight.getCurrentState(), "AUCUNE"));

            for (Vehicle v : new ArrayList<>(lane1.getVehicles())) {
                v.bdiCycle(lane1, road);
            }
            lane1.removeArrivedVehicles();

            displayLaneVehicles(lane1);

            if (i == 20) {
                System.out.println("⚠️ Injection de trafic additionnel !");
                for (int j = 0; j < 5; j++) {
                    Vehicle extra = new Vehicle(new Position(5 + j * 5, 1), new Position(100, 1), env);
                    lane1.addVehicle(extra);
                    vehicles.add(extra);
                }
            }

            sleep(500);
        }

        displayMetrics(vehicles);
    }

    public static void runScenario4() {
        System.out.println("\n=== SCÉNARIO 4 : Choix du mode de transport via argumentation ===");

        /* --------- 1. Contexte aléatoire --------- */
        Random rand = new Random();
        int startX = rand.nextInt(20);           // 0 – 19
        int destX = 50 + rand.nextInt(60);      // 50 – 109
        Position startPos = new Position(startX, 0);
        Position destPos = new Position(destX, 0);
        double dist = startPos.distanceTo(destPos);

        String[] weathers = {"Sunny", "Rainy", "Cloudy"};
        String weather = weathers[rand.nextInt(weathers.length)];
        boolean isHealthy = rand.nextBoolean();
        boolean isRushHour = rand.nextBoolean();


        System.out.printf(
                "Contexte : Distance ≈ %.1f unités | Météo = %s | Santé = %s | Heure de pointe = %s%n",
                dist, weather, isHealthy ? "OK" : "Faible", isRushHour ? "Oui" : "Non"
        );

        /* --------- 2. Construction de l’agent --------- */
        TransportationAgent agent =
                new TransportationAgent(startPos, destPos, weather, isHealthy, isRushHour);

        /* --------- 3. Décision et probabilités --------- */
        String decision = agent.decideTransportationMode();
        //Map<String, Double> probs = agent.getModeProbabilities();
        Map<String, Double> scrResults = agent.getModeScoresScr();

        System.out.println("\nDistribution des probabilités :");
        //probs.forEach((m, p) -> System.out.printf("  • %-17s : %.2f %%%n", m, p));
        scrResults.forEach((m, v) -> System.out.printf("  • %-17s : %.2f %%\n", m, v));

        System.out.println("\n Décision de l’agent → " + decision);

        /* --------- 4. Arguments acceptés (Grounded) ----- */
        DungTheory theory = agent.buildFramework();                // wrapper public → buildFramework()
        Extension accepted = new SimpleGroundedReasoner()
                .getModel(theory);

        System.out.println("\n Arguments acceptés (Grounded) :");
        accepted.forEach(a -> System.out.println("  - " + a.toString()));

        /* --------- 5. Affichage + export du graphe ------- */
        DungGraphPanel panel = new DungGraphPanel(theory, accepted);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Graphe d’argumentation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            DungGraphPanel.GraphExporter.exportPanelAsPNG(panel, "graph_export.png");
        });

        /* --------- 6. Comparaison rapide des sémantiques - */
        System.out.println("\n🔍 Comparaison des sémantiques :");
        agent.compareSemantics().forEach(
                (name, ext) -> System.out.printf("  - %-9s : %s%n", name, ext)
        );
    }

    public static void runBatchSimulation() {
        System.out.println("\n=== SIMULATION MASSIVE (100 décisions - SCR based) ===");

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String mode : List.of("CAR", "PUBLIC_TRANSPORT", "WALK", "BIKE"))
            counts.put(mode, 0);

        Random rand = new Random();

        for (int i = 0; i < 100; i++) {
            int startX = rand.nextInt(20);
            int destX = 50 + rand.nextInt(60);
            Position startPos = new Position(startX, 0);
            Position destPos = new Position(destX, 0);

            String[] weathers = {"Sunny", "Rainy", "Cloudy"};
            String weather = weathers[rand.nextInt(weathers.length)];
            boolean isHealthy = rand.nextBoolean();
            boolean isRush = rand.nextBoolean();

            TransportationAgent agent = new TransportationAgent(startPos, destPos, weather, isHealthy, isRush);
            String decision = agent.decideTransportationMode();  // utilise getModeScoresScr()

            counts.put(decision, counts.getOrDefault(decision, 0) + 1);
        }

        System.out.println("\n📊 Répartition après 100 décisions (SCR-based):");
        counts.forEach((mode, count) -> {
            double percent = (count / 100.0) * 100;
            System.out.printf("• %-17s : %3d (%.1f%%)%n", mode, count, percent);
        });
    }



    private static void displayLaneVehicles(Lane lane) {
        if (!lane.getVehicles().isEmpty()) {
            System.out.println("\n🚗 État de la lane " + lane.getId() + ":");
            for (Vehicle vehicle : lane.getVehicles()) {
                System.out.printf("- V%d @%s → %s\n",
                        vehicle.getId(),
                        vehicle.getPosition(),
                        vehicle.getAllIntentions().isEmpty() ? "(Aucune intention)" :
                                vehicle.getAllIntentions().get(vehicle.getAllIntentions().size() - 1));
            }
        }
    }

    private static void displayMetrics(List<Vehicle> vehicles) {
        System.out.println("\n=== Métriques de simulation ===");
        double totalTime = 0;
        int totalLaneChanges = 0;
        int totalFrustration = 0;
        int arrived = 0;

        for (Vehicle v : vehicles) {
            totalTime += v.getTravelTimeSeconds();
            totalLaneChanges += v.getLaneChangeCount();
            totalFrustration += v.getFrustrationCount();
            if (v.getBeliefs().contains("AtDestination", true)) arrived++;
        }

        int total = vehicles.size();
        System.out.printf("Temps moyen de trajet : %.2fs\n", total == 0 ? 0 : totalTime / total);
        System.out.printf("Changements de voie moyen : %.2f\n", total == 0 ? 0 : (double) totalLaneChanges / total);
        System.out.printf("Frustration moyenne : %.2f\n", total == 0 ? 0 : (double) totalFrustration / total);
        System.out.printf("Véhicules arrivés : %d/%d\n", arrived, total);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
