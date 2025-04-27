package org.example.environment;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TransitionMatrix {
    private final Random random = new Random();
    final Map<String, Map<String, Double>> transitions;
    private final double[][] trafficChangeMatrix; //matrice pour l'évolution du trafic

    public TransitionMatrix() {
        this.transitions = new HashMap<>();
        //matrice plus progressive pour l'évolution du trafic
        this.trafficChangeMatrix = new double[][]{
                // NONE   MEDIUM  HEAVY
                {0.5,    0.3,    0.2},   // Après NONE
                {0.4,    0.3,    0.3},    // Après MEDIUM
                {0.2,    0.4,    0.4}      // Après HEAVY
        };
        initializeRealisticTransitions();
    }

    private void initializeRealisticTransitions() {
        String[] colors = {"GREEN", "ORANGE", "RED"};
        String[] levels = {"NONE", "MEDIUM", "HEAVY"};

        for (String color : colors) {
            for (String currentLevel : levels) {
                String currentState = color + "_" + currentLevel;
                Map<String, Double> stateTransitions = new HashMap<>();
                int currentLevelIdx = TrafficLight.TrafficLevel.valueOf(currentLevel).ordinal();

                switch (color) {
                    case "GREEN" -> {
                        stateTransitions.put("GREEN_" + currentLevel, 0.7);      // reste vert
                        stateTransitions.put("ORANGE_" + currentLevel, 0.3);     // passe à orange
                    }
                    case "ORANGE" -> {
                        stateTransitions.put("RED_" + currentLevel, 1.0);        // passe toujours à rouge
                    }
                    case "RED" -> {
                        stateTransitions.put("RED_" + currentLevel, 0.6);        // reste rouge
                        stateTransitions.put("GREEN_" + currentLevel, 0.4);      // passe à vert
                    }
                }
                // 2)Transition de trafic plus progressive
                for (String nextLevel : levels) {
                    int nextLevelIdx = TrafficLight.TrafficLevel.valueOf(nextLevel).ordinal();
                    double trafficProb = trafficChangeMatrix[currentLevelIdx][nextLevelIdx];
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

    private TrafficLight.LightColor applyAction(TrafficLight.LightColor current, String action) {
        switch (action) {
            case "SWITCH_GREEN": return TrafficLight.LightColor.GREEN;
            case "SWITCH_ORANGE": return TrafficLight.LightColor.ORANGE;
            case "SWITCH_RED": return TrafficLight.LightColor.RED;
            default: return current;
        }
    }

    public double getProbability(String currentState, String nextState) {
        return transitions.getOrDefault(currentState, new HashMap<>())
                .getOrDefault(nextState, 0.0);
    }

    private TrafficLight.TrafficLevel getNextTrafficLevel(TrafficLight.TrafficLevel current) {
        double rand = random.nextDouble();
        double[] probs = trafficChangeMatrix[current.ordinal()];

        if (rand < probs[0]) return TrafficLight.TrafficLevel.NONE;
        if (rand < probs[0] + probs[1]) return TrafficLight.TrafficLevel.MEDIUM;
        return TrafficLight.TrafficLevel.HEAVY;
    }

    public double[] getTrafficTransitionProbs(TrafficLight.TrafficLevel level) {
        return trafficChangeMatrix[level.ordinal()];
    }

    //pourra m'être utile:

    public String getNextState(String currentState, String action) {
        String[] parts = currentState.split("_");
        TrafficLight.LightColor currentColor = TrafficLight.LightColor.valueOf(parts[0]);
        TrafficLight.TrafficLevel currentLevel = TrafficLight.TrafficLevel.valueOf(parts[1]);

        // 1) Déterminer la nouvelle couleur basée sur l'action
        TrafficLight.LightColor newColor = applyAction(currentColor, action);

        // 2)Déterminer le nouveau niveau de trafic (dépend du niveau actuel)
        TrafficLight.TrafficLevel newLevel = getNextTrafficLevel(currentLevel);

        return newColor + "_" + newLevel;
    }
}