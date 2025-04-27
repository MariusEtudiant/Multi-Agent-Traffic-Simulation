package org.example.environment;

import org.example.agent.Position;

import java.io.PrintWriter;
import java.util.*;

import static org.example.environment.TrafficLight.LightColor.GREEN;

/*
a trafficLight with an id, state, interval
 */
public class TrafficLight {


    public enum TrafficLevel {NONE, MEDIUM, HEAVY}
    public enum LightColor { GREEN, ORANGE, RED}

    private String id;
    private LightColor state;
    private int changeInterval = 10;
    private int stepCount = 0;

    private TrafficLevel currentTraffic;
    private Map<String, String> policy; //learned policy
    private double[][] valueFunction;
    private boolean useMDP = true; //par défaut on utilise le MDP


    // Q-learning
    private Map<String, Map<String, Double>> qTable;
    private double alpha = 0.5; // learning rate
    private double gamma = 0.9; //discount factor
    private double epsilon = 0.3;//exploration rate


    private final TransitionMatrix transitionMatrix = new TransitionMatrix();
    private Position position;


    public TrafficLight(String id, LightColor state) {
        this.id = id;
        this.state = state;
        this.currentTraffic = TrafficLevel.NONE;
        this.qTable = initializeQTable();
    }

    private Map<String, Map<String, Double>> initializeQTable() {
        Map<String, Map<String, Double>> table = new HashMap<>();
        for (LightColor color : LightColor.values()) {
            for (TrafficLevel level : TrafficLevel.values()) {
                String state = getStateKey(color, level);
                table.put(state, new HashMap<>());
                for (String action : getPossibleActions(color)) {
                    table.get(state).put(action, 0.0);
                }
            }
        }
        return table;
    }

    private String getStateKey(LightColor color, TrafficLevel level) {
        return color.toString() + "_" + level.toString();
    }

    private List<String> getPossibleActions(LightColor currentColor) {
        List<String> actions = new ArrayList<>();

        switch (currentColor) {
            case GREEN -> {
                actions.add("STAY_GREEN");
                actions.add("SWITCH_ORANGE");
            }
            case ORANGE -> {
                actions.add("STAY_ORANGE");
                actions.add("SWITCH_RED");
            }
            case RED -> {
                actions.add("STAY_RED");
                actions.add("SWITCH_GREEN");
            }
        }
        return actions;
    }



    public void updateTrafficLevel(int vehicleCount) {
        if (vehicleCount == 0) {
            currentTraffic = TrafficLevel.NONE;
        } else if (vehicleCount < 5) {
            currentTraffic = TrafficLevel.MEDIUM;
        } else {
            currentTraffic = TrafficLevel.HEAVY;
        }
    }

    public void mdpUpdate() {
        // Get current state
        String currentState = getStateKey(state, currentTraffic);

        // Choose action (ε-greedy for Q-learning)
        String action;
        if (Math.random() < epsilon) {
            // Explore: random action
            List<String> possibleActions = getPossibleActions(state);
            action = possibleActions.get((int)(Math.random() * possibleActions.size()));
        } else {
            // Exploit: best known action
            action = getBestAction(currentState);
        }

        // Execute action
        executeAction(action);

        // Observe reward and new state
        double reward = calculateReward();
        String newState = getStateKey(state, currentTraffic);

        // Q-learning update
        double maxQNewState = getMaxQValue(newState);
        double currentQ = qTable.get(currentState).get(action);
        double newQ = currentQ + alpha * (reward + gamma * maxQNewState - currentQ);
        qTable.get(currentState).put(action, newQ);
    }

    private String getBestAction(String state) {
        Map<String, Double> stateActions = qTable.get(state);
        return Collections.max(stateActions.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private double getMaxQValue(String state) {
        return Collections.max(qTable.get(state).values());
    }

    public void executeAction(String action) {

        switch (action) {
            case "STAY_RED":
            case "STAY_GREEN":
            case "STAY_ORANGE":
                // No color change
                break;
            case "SWITCH_GREEN":
                this.state = GREEN;
                break;
            case "SWITCH_ORANGE":
                this.state = LightColor.ORANGE;
                break;
            case "SWITCH_RED":
                this.state = LightColor.RED;
                break;
        }
    }
    private double calculateReward() {
        return calculateReward(state, currentTraffic);
    }

    private double calculateReward(LightColor color, TrafficLevel level) {
        // Récompenses plus équilibrées qui encouragent les changements quand nécessaire
        switch (color) {
            case GREEN:
                switch (level) {
                    case NONE: return 1.0;   // Bonne situation
                    case MEDIUM: return 3.0; // Meilleure récompense pour trafic moyen (feu vert utile)
                    case HEAVY: return 5.0; // Légère pénalité pour trafic lourd
                }
            case ORANGE:
                return -1.0; // Pénalité modérée pour orange
            case RED:
                return switch (level) {
                    case NONE -> -3.0; // Encourager le changement
                    case MEDIUM -> -6.0;
                    case HEAVY -> -10.0;
                };

            default: return 0.0;
        }
    }


    public void valueIteration(double theta) {
        valueFunction = new double[LightColor.values().length][TrafficLevel.values().length];
        int maxIterations = 2000;
        double gamma = 0.8;  // Réduire encore

        for (int i = 0; i < maxIterations; i++) {
            double delta = 0;
            for (LightColor color : LightColor.values()) {
                for (TrafficLevel level : TrafficLevel.values()) {
                    double oldValue = valueFunction[color.ordinal()][level.ordinal()];
                    double maxValue = Double.NEGATIVE_INFINITY;

                    for (String action : getPossibleActions(color)) {
                        double actionValue = calculateActionValue(color, level, action, gamma);
                        maxValue = Math.max(maxValue, actionValue);
                    }

                    valueFunction[color.ordinal()][level.ordinal()] = maxValue;
                    delta = Math.max(delta, Math.abs(oldValue - maxValue));
                }
            }
            if (delta < theta) break;
        }
        extractPolicy();
    }


    private LightColor applyAction(LightColor current, String action) {
        switch (action) {
            case "SWITCH_GREEN": return LightColor.GREEN;
            case "SWITCH_ORANGE": return LightColor.ORANGE;
            case "SWITCH_RED": return LightColor.RED;
            default: return current;
        }
    }

    private double calculateActionValue(LightColor color, TrafficLevel level, String action, double gamma) {
        double total = 0.0;

        //1) Couleur suivante déterminée par l'action
        LightColor nextColor = applyAction(color, action);
        //2) Probabilités d'évolution du trafic (indépendantes de l'action)
        double[] trafficProbs = getTrafficTransitionProbs(level);
        //3) Boucle sur les niveaux de trafic possibles
        for (int i = 0; i < trafficProbs.length; i++) {
            TrafficLevel nextLevel = TrafficLevel.values()[i];
            double probability = trafficProbs[i];

            double reward = calculateReward(nextColor, nextLevel) + getActionPenalty(action);
            double futureValue = valueFunction[nextColor.ordinal()][nextLevel.ordinal()];

            total += probability * (reward + gamma * futureValue);
        }
        return total;
    }
    private double getActionPenalty(String action) {
        //récompense neutre ou bonus pour rester stable, pénalité pour changer
        if (action.startsWith("STAY_")) return 0.5;
        return -2.0; //pénalité pour éviter les changements trop fréquents
    }

    private void extractPolicy() {
        policy = new HashMap<>();
        double gammaLocal = 0.8; //même valeur que dans valueIteration

        for (LightColor color : LightColor.values()) {
            for (TrafficLevel level : TrafficLevel.values()) {
                String stateKey = getStateKey(color, level);
                double bestValue = Double.NEGATIVE_INFINITY;
                String bestAction = "STAY_" + color.name(); // valeur par défaut

                for (String action : getPossibleActions(color)) {
                    double actionValue = calculateActionValue(color, level, action, gammaLocal);
                    if (actionValue > bestValue) {
                        bestValue = actionValue;
                        bestAction = action;
                    }
                }

                policy.put(stateKey, bestAction);
                valueFunction[color.ordinal()][level.ordinal()] = bestValue;
            }
        }
    }

    public void printTransitionMatrix() {
        System.out.println("Matrice de Transition Complète:");
        System.out.printf("%-15s", "État Actuel");
        for (TrafficLevel level : TrafficLevel.values()) {
            for (LightColor color : LightColor.values()) {
                System.out.printf("%-15s", color + "_" + level);
            }
        }
        System.out.println();

        for (String fromState : transitionMatrix.transitions.keySet()) {
            System.out.printf("%-15s", fromState);
            for (TrafficLevel level : TrafficLevel.values()) {
                for (LightColor color : LightColor.values()) {
                    String toState = color + "_" + level;
                    System.out.printf("%-15.2f", transitionMatrix.getProbability(fromState, toState));
                }
            }
            System.out.println();
        }
    }

    public void printPolicy() {
        System.out.println("\n=== Politique Optimale ===");
        System.out.printf("%-20s | %-20s | %-15s\n", "État", "Action Optimale", "Valeur");
        System.out.println("---------------------|----------------------|---------------");

        for (LightColor color : LightColor.values()) {
            for (TrafficLevel level : TrafficLevel.values()) {
                String state = getStateKey(color, level);
                String action = policy.getOrDefault(state, "AUCUNE_ACTION");
                double value = valueFunction[color.ordinal()][level.ordinal()];

                System.out.printf("%-20s | %-20s | %-15.2f\n",
                        state,
                        action,
                        value);
            }
        }
    }

    public void updatePolicy() {
        this.valueIteration(0.005); //recalculer avec la nouvelle matrice
        this.extractPolicy();
    }

    public void setState(LightColor state) {
        this.state = state;
    }
    public void setUseMDP(boolean useMDP) {
        this.useMDP = useMDP;
    }

    public void update() {
        stepCount++;
        if (stepCount % changeInterval == 0) {
            if (!useMDP) {
                toggleState();  //cyclique seulement en mode manuel
            }
            stepCount = 0;
        }
    }
    public boolean isUseMDP() {
        return useMDP;
    }

    public void toggleState() {
        if (useMDP) {
            //ne rien faire : c’est le MDP qui contrôle
            return;
        }
        //sinon, comportement par défaut
        switch (state) {
            case GREEN -> state = LightColor.ORANGE;
            case ORANGE -> state = LightColor.RED;
            case RED -> state = LightColor.GREEN;
        }
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public void resetLearning() {
        if (qTable != null) {
            for (String key : qTable.keySet()) {
                Map<String, Double> actionValues = qTable.get(key);
                for (String action : actionValues.keySet()) {
                    actionValues.put(action, 0.0);
                }
            }
        }

        if (valueFunction != null) {
            for (int i = 0; i < valueFunction.length; i++) {
                for (int j = 0; j < valueFunction[i].length; j++) {
                    valueFunction[i][j] = 0.0;
                }
            }
        }
    }

    //gets
    public Map<String, Double> getValueFunctionAsMap() {
        Map<String, Double> map = new LinkedHashMap<>();
        for (LightColor color : LightColor.values()) {
            for (TrafficLevel level : TrafficLevel.values()) {
                String key = color.name() + "_" + level.name();
                double value = valueFunction[color.ordinal()][level.ordinal()];
                map.put(key, value);
            }
        }
        return map;
    }

    public Map<String, Double> getQTableMaxValuesAsMap() {
        Map<String, Double> map = new LinkedHashMap<>();
        if (qTable == null) return map;

        for (String state : qTable.keySet()) {
            double maxQ = qTable.get(state).values().stream().max(Double::compareTo).orElse(0.0);
            map.put(state, maxQ);
        }
        return map;
    }

    private double[] getTrafficTransitionProbs(TrafficLevel currentLevel) {
        return transitionMatrix.getTrafficTransitionProbs(currentLevel);
    }

    public Position getPosition() {
        return position;
    }
    public LightColor getState(){
        return state;
    }
    public String getId() {
        return id;
    }
    public TrafficLevel getCurrentTraffic() {
        return currentTraffic;
    }


    //pourra être utile:
    public void exportPolicyToCSV(String filePath) {
        try (PrintWriter writer = new PrintWriter(filePath)) {
            writer.println("TrafficLevel,GREEN,ORANGE,RED");

            for (TrafficLevel level : TrafficLevel.values()) {
                StringBuilder line = new StringBuilder(level.name());
                for (LightColor color : LightColor.values()) {
                    String state = getStateKey(color, level);
                    String action = policy.getOrDefault(state, "AUCUNE");
                    line.append(",").append(action);
                }
                writer.println(line);
            }

            System.out.println("Politique exportée vers : " + filePath);
        } catch (Exception e) {
            System.out.println("Erreur d'export : " + e.getMessage());
        }
    }
    public String getCurrentState() {
        return getStateKey(state, currentTraffic);
    }
    public Map<String, String> getPolicy() {
        return policy;
    }

}
