package org.example.environment;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TransitionMatrix {
    private final Random random = new Random();
    final Map<String, Map<String, Double>> transitions;
    private final double[][] trafficChangeMatrix; // Nouvelle matrice pour l'évolution du trafic

    public TransitionMatrix() {
        this.transitions = new HashMap<>();
        // Matrice de changement de trafic: P(next_traffic | current_traffic)
        // Matrice plus progressive pour l'évolution du trafic
        this.trafficChangeMatrix = new double[][]{
                // NONE   MEDIUM  HEAVY
                {0.5,    0.3,    0.2},   // Après NONE
                {0.4,    0.3,    0.3},    // Après MEDIUM
                {0.2,    0.4,    0.4}      // Après HEAVY
        };
        initializeRealisticTransitions();
    }
    public double[] getTrafficTransitionProbs(TrafficLight.TrafficLevel level) {
        return trafficChangeMatrix[level.ordinal()];
    }

    private void initializeRealisticTransitions() {
        String[] colors = {"GREEN", "ORANGE", "RED"};
        String[] levels = {"NONE", "MEDIUM", "HEAVY"};

        for (String color : colors) {
            for (String currentLevel : levels) {
                String currentState = color + "_" + currentLevel;
                Map<String, Double> stateTransitions = new HashMap<>();
                int currentLevelIdx = TrafficLight.TrafficLevel.valueOf(currentLevel).ordinal();

                // 1. Transition de couleur plus réaliste
                if ("ORANGE".equals(color)) {
                    // Orange passe principalement à Rouge (80%) ou parfois à Vert (20%)
                    stateTransitions.put("RED_" + currentLevel, 0.8);
                    stateTransitions.put("GREEN_" + currentLevel, 0.2);
                } else if ("GREEN".equals(color)) {
                    // Vert reste Vert (70%) ou passe à Orange (30%)
                    stateTransitions.put("GREEN_" + currentLevel, 0.7);
                    stateTransitions.put("ORANGE_" + currentLevel, 0.3);
                } else { // RED
                    // Rouge reste Rouge (60%) ou passe à Vert (40%)
                    stateTransitions.put("RED_" + currentLevel, 0.6);
                    stateTransitions.put("GREEN_" + currentLevel, 0.4);
                }
                // 2. Transition de trafic plus progressive
                for (String nextLevel : levels) {
                    int nextLevelIdx = TrafficLight.TrafficLevel.valueOf(nextLevel).ordinal();
                    double trafficProb = trafficChangeMatrix[currentLevelIdx][nextLevelIdx];

                    // Répartir les probabilités de manière plus équilibrée
                    for (String nextColor : colors) {
                        String nextState = nextColor + "_" + nextLevel;
                        // Réduire l'impact des transitions extrêmes
                        stateTransitions.merge(nextState, trafficProb * 0.3, Double::sum);
                    }
                }

                transitions.put(currentState, normalizeProbabilities(stateTransitions));
            }
        }
    }

    private Map<String, Double> normalizeProbabilities(Map<String, Double> probs) {
        double total = probs.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<String, Double> normalized = new HashMap<>();
        probs.forEach((state, prob) -> normalized.put(state, prob / total));
        return normalized;
    }

    public double getProbability(String currentState, String nextState) {
        return transitions.getOrDefault(currentState, new HashMap<>())
                .getOrDefault(nextState, 0.0);
    }

    public String getNextState(String currentState, String action) {
        String[] parts = currentState.split("_");
        TrafficLight.LightColor currentColor = TrafficLight.LightColor.valueOf(parts[0]);
        TrafficLight.TrafficLevel currentLevel = TrafficLight.TrafficLevel.valueOf(parts[1]);

        // 1. Déterminer la nouvelle couleur basée sur l'action
        TrafficLight.LightColor newColor = applyAction(currentColor, action);

        // 2. Déterminer le nouveau niveau de trafic (dépend du niveau actuel)
        TrafficLight.TrafficLevel newLevel = getNextTrafficLevel(currentLevel);

        return newColor + "_" + newLevel;
    }

    private TrafficLight.LightColor applyAction(TrafficLight.LightColor current, String action) {
        switch (action) {
            case "SWITCH_GREEN": return TrafficLight.LightColor.GREEN;
            case "SWITCH_ORANGE": return TrafficLight.LightColor.ORANGE;
            case "SWITCH_RED": return TrafficLight.LightColor.RED;
            default: return current;
        }
    }

    private TrafficLight.TrafficLevel getNextTrafficLevel(TrafficLight.TrafficLevel current) {
        double rand = random.nextDouble();
        double[] probs = trafficChangeMatrix[current.ordinal()];

        if (rand < probs[0]) return TrafficLight.TrafficLevel.NONE;
        if (rand < probs[0] + probs[1]) return TrafficLight.TrafficLevel.MEDIUM;
        return TrafficLight.TrafficLevel.HEAVY;
    }
}