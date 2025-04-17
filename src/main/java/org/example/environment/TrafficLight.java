package org.example.environment;

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
    private Map<String, String> policy; // Learned policy
    private double[][] valueFunction; // State values

    // Q-learning parameters
    private Map<String, Map<String, Double>> qTable;
    private double alpha = 0.2; // Learning rate
    private double gamma = 0.9; // Discount factor
    private double epsilon = 0.2; // Exploration rate

    private final TransitionMatrix transitionMatrix = new TransitionMatrix();


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
            case RED:
                actions.add("STAY_RED");
                actions.add("SWITCH_GREEN");
                break;
            case GREEN:
                actions.add("STAY_GREEN");
                actions.add("SWITCH_ORANGE");
                break;
            case ORANGE:
                actions.add("STAY_ORANGE");
                actions.add("SWITCH_RED");
                break;
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

        switch (color) {
            case GREEN:
                switch (level) {
                    case NONE: return 1.0;
                    case MEDIUM: return 2.0;
                    case HEAVY: return 0.5;
                }
            case ORANGE:
                return -1.0;
            case RED:
                return level == TrafficLevel.NONE ? -1.0 :
                        (level == TrafficLevel.MEDIUM ? -3.0 : -8.0);
            default:
                return 0.0;
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
    private double getActionPenalty(String action) {
        return action.startsWith("STAY_") ? -1.5 : 0; // Pénalité importante pour rester
    }

    private double calculateActionValue(LightColor color, TrafficLevel level, String action, double gamma) {
        double total = 0;
        String state = getStateKey(color, level);

        // Pénalité d'inaction ajoutée
        double actionPenalty = getActionPenalty(action);

        for (LightColor newColor : LightColor.values()) {
            for (TrafficLevel newLevel : TrafficLevel.values()) {
                String nextState = getStateKey(newColor, newLevel);
                double prob = transitionMatrix.getProbability(state, nextState);
                double reward = calculateReward(newColor, newLevel) + actionPenalty;
                total += prob * (reward + gamma * valueFunction[newColor.ordinal()][newLevel.ordinal()]);
            }
        }
        return total;
    }

    private void extractPolicy() {
        policy = new HashMap<>();

        for (LightColor color : LightColor.values()) {
            for (TrafficLevel level : TrafficLevel.values()) {
                double maxValue = Double.NEGATIVE_INFINITY;
                String bestAction = null;

                for (String action : getPossibleActions(color)) {
                    double actionValue = 0;

                    // Utiliser la vraie matrice de transition
                    for (LightColor newColor : LightColor.values()) {
                        for (TrafficLevel newLevel : TrafficLevel.values()) {
                            String state = getStateKey(color, level);
                            String nextState = getStateKey(newColor, newLevel);
                            double prob = transitionMatrix.getProbability(state, nextState);
                            double reward = calculateReward(newColor, newLevel);
                            actionValue += prob * (reward + gamma * valueFunction[newColor.ordinal()][newLevel.ordinal()]);
                        }
                    }

                    if (actionValue > maxValue) {
                        maxValue = actionValue;
                        bestAction = action;
                    }
                }

                policy.put(getStateKey(color, level), bestAction);
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
        this.valueIteration(0.001); // Recalculer avec la nouvelle matrice
        this.extractPolicy();
    }


    // fonctions de bases
    public String getCurrentState() {
        return getStateKey(state, currentTraffic);
    }
    public Map<String, String> getPolicy() {
        return policy;
    }

    public TrafficLevel getCurrentTraffic() {
        return currentTraffic;
    }

    public LightColor getState(){
        return state;
    }
    public void setState(LightColor state) {
        this.state = state;
    }
    public String getId() {
        return id;
    }

    public void update(){
        stepCount++;
        if (stepCount % changeInterval == 0) {
            toggleState();
            stepCount = 0;
        }
    }

    public void toggleState() {
        switch (state) {
            case GREEN:
                state = LightColor.ORANGE;
                break;
            case ORANGE:
                state = LightColor.RED;
                break;
            case RED:
                state = GREEN;
                break;
        }
    }
}
