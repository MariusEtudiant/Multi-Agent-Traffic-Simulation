
package org.example;

import org.example.ArgumentationDM.DungGraphPanel;
import org.example.ArgumentationDM.TransportationAgent;

import org.example.agent.Position;
import org.tweetyproject.arg.dung.reasoner.SimpleGroundedReasoner;
import org.tweetyproject.arg.dung.semantics.Extension;
import org.tweetyproject.arg.dung.syntax.DungTheory;
import javax.swing.*;
import java.util.*;

public class Semantique {
    public static void main(String[] args) {
        System.out.println("Démarrage de la simulation...");
        //runScenario4();
        //runBatchSimulation();
        runMultipleScenarios();
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
        String decision = agent.decideTransportationModeWithoutSCR();

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

    public static void runMultipleScenarios() {
        System.out.println("\n=== TEST DE MULTIPLES SCÉNARIOS (Génération Graphique) ===");

        List<String> weathers = List.of("Sunny", "Rainy", "Cloudy");
        Random rand = new Random();

        for (int i = 1; i <= 5; i++) {   // Génère 5 contextes différents
            System.out.println("\n--- SCÉNARIO " + i + " ---");

            // Contexte aléatoire
            int startX = rand.nextInt(20);
            int destX = 50 + rand.nextInt(60);
            Position startPos = new Position(startX, 0);
            Position destPos = new Position(destX, 0);
            String weather = weathers.get(rand.nextInt(weathers.size()));
            boolean isHealthy = rand.nextBoolean();
            boolean isRushHour = rand.nextBoolean();

            TransportationAgent agent = new TransportationAgent(startPos, destPos, weather, isHealthy, isRushHour);

            // Décision sans SCR
            String decision = agent.decideTransportationModeWithoutSCR();
            System.out.println("Décision : " + decision);

            // Graphe et extension acceptée
            DungTheory theory = agent.getFramework();
            Extension accepted = new SimpleGroundedReasoner().getModel(theory);

            // Création panneau et export
            DungGraphPanel panel = new DungGraphPanel(theory, accepted);

            int idx = i;  // copie pour usage dans Swing
            SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame("Scenario " + idx);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.add(panel);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

                String filename = "graph_scenario_" + idx + ".png";
                DungGraphPanel.GraphExporter.exportPanelAsPNG(panel, filename);
            });
        }
    }

}
