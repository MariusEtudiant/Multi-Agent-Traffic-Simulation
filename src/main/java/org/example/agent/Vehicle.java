package org.example.agent;

import org.example.environment.*;
import org.example.logic.*;
import org.example.planning.DijkstraAlgorithm;
import org.example.planning.Graph;
import org.example.planning.GraphNode;

import java.util.*;

/**
 * Vehicle agent class using a BDI (Belief-Desire-Intention) architecture to navigate a traffic simulation.
 * It maintains its own beliefs, evaluates desires (goals), and executes intentions (actions).
 */
public class Vehicle {

    /** Transport modes the agent can simulate. */
    public enum TransportationMode { CAR, BIKE, PUBLIC_TRANSPORT, WALK }

    // === State Fields ===
    private static int nextId = 1;
    private final int id;
    private TransportationMode mode = TransportationMode.CAR;
    private double speedFactor = 1.0;

    private Position position;
    private double preciseX;
    private Position destination;
    private Position previousPosition;
    private Lane currentLane;
    private Road road;

    private BeliefInitial beliefs;
    private List<Desire> desires;
    private List<Goal> goals;
    private Queue<Intention> intentions;
    private List<Intention> executedIntentions = new ArrayList<>();

    private List<Position> path = new ArrayList<>();
    private int nextWaypointIdx = 0;
    private Position lastPlannedPosition = null;

    private Environment environment;
    private boolean useGlobalGraph = true;
    private long lastPlanTime = 0;
    private static final long PLAN_COOLDOWN_MS = 1000;
    private long lastLaneChangeTime = 0;
    private static final long LANE_CHANGE_COOLDOWN_MS = 100;

    // Metrics
    private long startTime;
    private Long endTime = null;
    private int laneChangeCount = 0;
    private int frustrationCount = 0;

    // === Constructor ===
    public Vehicle(Position position, Position destination, Environment environment) {
        this.id = nextId++;
        this.position = position;
        this.preciseX = position.getX();
        this.destination = destination;
        this.environment = environment;
        this.startTime = System.currentTimeMillis();

        this.beliefs = new BeliefInitial();
        this.desires = new ArrayList<>();
        this.goals = new ArrayList<>();
        this.intentions = new LinkedList<>();

        initializeGoals();
    }

    // === Setup ===
    private void initializeGoals() {
        goals.add(new Goal(new Desire("REACH_DESTINATION", 1), new AtomFormula("AtDestination", true)));
        goals.add(new Goal(new Desire("OBEY_TRAFFIC_RULES", 1), new AtomFormula("TrafficRulesObeyed", true)));
        goals.add(new Goal(new Desire("AVOID_COLLISION", 2), new NotFormula(new AtomFormula("CollisionRisk", true))));
        goals.add(new Goal(new Desire("AVOID_TRAFFIC_JAM", 2), new NotFormula(new AtomFormula("InTrafficJam", true))));

        for (Goal goal : goals) {
            desires.add(goal.getDesire());
        }
    }

    public void setMode(TransportationMode mode) {
        this.mode = mode;
        this.speedFactor = switch (mode) {
            case CAR -> 1.0;
            case BIKE -> 0.6;
            case PUBLIC_TRANSPORT -> 0.8;
            case WALK -> 0.2;
        };
    }

    // === BDI Cycle ===
    public void bdiCycle(Lane lane, Road road) {
        this.currentLane = lane;
        this.road = road;
        this.previousPosition = new Position(position.getX(), position.getY());

        perceivedEnvironment(lane, road);
        updateDesires();
        deliberate();
        planIfNeeded();
        act();
        updatePostActionBeliefs();
    }

    private void perceivedEnvironment(Lane lane, Road road) {
        beliefs.updateBeliefs(lane, road, this);

        double distanceToDest = position.distanceTo(destination);
        boolean arrived = distanceToDest < 5.0 && position.getY() == destination.getY();
        beliefs.addBelief(new Belief("AtDestination", arrived));
        beliefs.addBelief(new Belief("NearDestination", distanceToDest < 15.0));
        beliefs.addBelief(new Belief("HighSpeed", distanceToDest > 10 && lane.getVehicleSpeed(this) > 30.0));
        beliefs.addBelief(new Belief("CollisionRisk", beliefs.contains("CarAhead", true) && beliefs.contains("HighSpeed", true)));

        if (distanceToDest < 5.0) {
            desires.stream()
                    .filter(d -> !d.getName().equals("REACH_DESTINATION"))
                    .forEach(Desire::achieve);
        }
    }

    private void updateDesires() {
        desires.forEach(Desire::reset);
        for (Goal goal : goals) {
            if (goal.isAchieved(beliefs)) {
                goal.getDesire().achieve();
            } else if (!desires.contains(goal.getDesire())) {
                desires.add(goal.getDesire());
            }
        }
        desires.sort(Comparator.comparingInt(Desire::getPriority));
    }

    private void deliberate() {
        List<Desire> activeDesires = desires.stream()
                .filter(d -> !d.isAchieved())
                .sorted(Comparator.comparingInt(Desire::getPriority))
                .toList();

        if (!activeDesires.isEmpty()) {
            generateIntentions(activeDesires.get(0));
        }
    }

    private void planIfNeeded() {
        if (beliefs.contains("AtDestination", true)) return;

        boolean needsPlan = (path == null || path.isEmpty())
                || (nextWaypointIdx >= path.size())
                || beliefs.contains("ObstacleAhead", true)
                || (beliefs.contains("InTrafficJam", true) && (System.currentTimeMillis() - lastPlanTime > 5000));

        long now = System.currentTimeMillis();
        if (needsPlan && (now - lastPlanTime > PLAN_COOLDOWN_MS)) {
            plan();
            lastPlanTime = now;
        }
    }

    private void plan() {
        Graph roadGraph = (useGlobalGraph && environment != null)
                ? environment.getGlobalGraph()
                : road.getGraph();

        if (roadGraph == null) return;

        int snappedX = Math.round((float) position.getX() / 10) * 10;
        int startY = currentLane.getCenterYInt();
        int goalY = destination.getY();

        Position snappedStart = new Position(snappedX, startY);
        Position snappedGoal = new Position(Math.round((float) destination.getX() / 10) * 10, goalY);

        System.out.println("🧠 V" + id + " planning:");
        System.out.println("   position actuelle = " + position + ", lane Y = " + currentLane.getCenterYInt());
        System.out.println("   snappedStart = " + snappedStart);
        System.out.println("   destination = " + destination);
        System.out.println("   snappedGoal = " + snappedGoal);

        GraphNode startNode = roadGraph.getNode(snappedStart);
        GraphNode goalNode = roadGraph.getNode(snappedGoal);

        if (startNode == null || goalNode == null) {
            System.out.println("❌ StartNode ou GoalNode introuvable dans le graphe !");
            return;
        }

        List<Position> path = DijkstraAlgorithm.computePath(roadGraph, snappedStart, snappedGoal);

        System.out.println("📍 Chemin trouvé pour V" + id + ":");
        for (int i = 0; i < path.size(); i++) {
            System.out.println("   ➤ Waypoint " + i + ": " + path.get(i));
        }

        this.path = path;
        this.nextWaypointIdx = 0;
    }



    private void generateIntentions(Desire desire) {
        intentions.clear();
        Map<Intention, Integer> tempIntentions = new LinkedHashMap<>();

        double distanceToLight = getDistanceToNextLight();
        double brakingDistance = computeDynamicBrakingDistance(currentLane);

        // Logique BDI — formules
        LogicalFormula feuVert = new AtomFormula("FeuVert", true);
        LogicalFormula feuRouge = new AtomFormula("FeuRouge", true);
        LogicalFormula feuOrange = new AtomFormula("FeuOrange", true);
        LogicalFormula carAhead = new AtomFormula("CarAhead", true);
        LogicalFormula carLeft = new AtomFormula("CarOnLeft", true);
        LogicalFormula carRight = new AtomFormula("CarOnRight", true);
        LogicalFormula obstacle = new AtomFormula("ObstacleAhead", true);
        LogicalFormula inTrafficJam = new AtomFormula("InTrafficJam", true);
        LogicalFormula priorityVehicle = new AtomFormula("PriorityVehicle", true);
        LogicalFormula highSpeed = new AtomFormula("HighSpeed", true);

        // 🧭 Repositionnement vers waypoint.Y si possible
        if (path != null && nextWaypointIdx < path.size()) {
            Position target = path.get(nextWaypointIdx);
            int targetY = target.getY();

            if (position.getY() != targetY) {
                boolean canChangeLane = (System.currentTimeMillis() - lastLaneChangeTime > LANE_CHANGE_COOLDOWN_MS);
                boolean tryLeft = position.getY() > targetY;
                boolean hasLane = tryLeft ? road.hasLeftLane(currentLane) : road.hasRightLane(currentLane);
                boolean noCar = !beliefs.contains(tryLeft ? "CarOnLeft" : "CarOnRight", true);

                if (canChangeLane && hasLane && noCar) {
                    Intention turn = tryLeft ? Intention.TURN_LEFT : Intention.TURN_RIGHT;
                    System.out.println("↪️ V" + id + " repositionnement vers Y=" + targetY + " → " + turn);
                    tempIntentions.put(turn, -1);
                    lastLaneChangeTime = System.currentTimeMillis();
                } else {
                    System.out.println("❌ Repositionnement impossible vers Y=" + targetY + " → hasLane=" + hasLane + ", noCar=" + noCar);
                }
            }
        }

        // 🚦 Feux
        if (feuRouge.evaluate(beliefs) && beliefs.contains("FeuDevant", true) && !beliefs.contains("FeuFranchi", true)
                && distanceToLight > 0 && distanceToLight < brakingDistance + 4) {
            if (distanceToLight < brakingDistance * 0.5) {
                tempIntentions.put(Intention.STOP, 0);
            } else {
                tempIntentions.put(Intention.SLOW_DOWN, 0);
            }
        }

        if (feuOrange.evaluate(beliefs) && !beliefs.contains("FeuFranchi", true)
                && distanceToLight > 0 && distanceToLight < brakingDistance) {
            tempIntentions.put(Intention.SLOW_DOWN, 0);
        }

        // 🚨 Risques
        if (new AndFormula(carAhead, highSpeed).evaluate(beliefs)) {
            tempIntentions.put(Intention.STOP, 0);
        }

        if (new OrFormula(feuRouge, priorityVehicle).evaluate(beliefs)
                && distanceToLight > 0 && distanceToLight < 25) {
            tempIntentions.put(Intention.SLOW_DOWN, 0);
        }

        // 🚘 Navigation libre
        if ("REACH_DESTINATION".equals(desire.getName())) {
            LogicalFormula canAccelerate = new AndFormula(feuVert, new AndFormula(new NotFormula(carAhead), new NotFormula(obstacle)));
            if (canAccelerate.evaluate(beliefs)) {
                tempIntentions.put(Intention.ACCELERATE, 1);
            }

            LogicalFormula canOvertake = new AndFormula(carAhead, new OrFormula(new NotFormula(carLeft), new NotFormula(carRight)));
            if (obstacle.evaluate(beliefs) || canOvertake.evaluate(beliefs)) {
                boolean canTurnLeft = !carLeft.evaluate(beliefs) && road.hasLeftLane(currentLane);
                boolean canTurnRight = !carRight.evaluate(beliefs) && road.hasRightLane(currentLane);
                if (canTurnLeft) tempIntentions.put(Intention.TURN_LEFT, 1);
                else if (canTurnRight) tempIntentions.put(Intention.TURN_RIGHT, 1);
                else tempIntentions.put(Intention.SLOW_DOWN, 1);
            }
        }

        // 🧭 Suivi du chemin
        if (path != null && !path.isEmpty()) {
            Position target = path.get(nextWaypointIdx);
            double distanceToTarget = position.distanceTo(target);

            // Seuil dynamique basé sur la vitesse
            double threshold = Math.max(2.0, currentLane.getVehicleSpeed(this) * 0.1);
            if (distanceToTarget < threshold) {
                nextWaypointIdx = Math.min(nextWaypointIdx + 1, path.size() - 1);
                target = path.get(nextWaypointIdx);
            }

            // Calcul de la direction priorisant le chemin
            double dx = target.getX() - position.getX();
            double dy = target.getY() - position.getY();

            if (Math.abs(dx) > 1.0) { // Prioriser l'axe X
                tempIntentions.put(dx > 0 ? Intention.ACCELERATE : Intention.SLOW_DOWN, 2);
            } else if (Math.abs(dy) > 1.0) { // Ajuster Y si nécessaire
                boolean canChangeLane = (System.currentTimeMillis() - lastLaneChangeTime > LANE_CHANGE_COOLDOWN_MS);
                if (dy > 0 && road.hasRightLane(currentLane) && canChangeLane) {
                    tempIntentions.put(Intention.TURN_RIGHT, 2);
                } else if (dy < 0 && road.hasLeftLane(currentLane) && canChangeLane) {
                    tempIntentions.put(Intention.TURN_LEFT, 2);
                }
            }
        }

        // 🔁 Fallback
        if (tempIntentions.isEmpty()) {
            tempIntentions.put(Intention.ACCELERATE, 2);
        }

        tempIntentions.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .forEach(intentions::add);

        System.out.println("🎯 Intentions finales V" + id + " = " + intentions);
    }



    public void act() {
        if (!intentions.isEmpty()) {
            Intention intention = intentions.poll();
            executedIntentions.add(intention);
            executeIntention(intention);
        }
    }

    private void executeIntention(Intention intention) {
        if (currentLane == null) {
            System.out.println("Erreur : Aucune lane définie");
            return;
        }

        if (beliefs.contains("AtDestination", true)) {
            if (endTime == null) endTime = System.currentTimeMillis();
            System.out.println("✅ Véhicule arrivé à destination");
            return;
        }

        double distanceToLight = getDistanceToNextLight();
        double brakingDistance = computeDynamicBrakingDistance(currentLane);
        boolean isRedLightNear = beliefs.contains("FeuRouge", true)
                && distanceToLight > 0 && distanceToLight < brakingDistance;

        int directionFactor = (currentLane.getDirection() == Lane.DIRECTION_LEFT) ? -1 : 1;

        switch (intention) {
            case ACCELERATE -> {
                if (isRedLightNear) {
                    System.out.println("⛔ V" + id + " voulait accélérer mais feu rouge proche ! STOP forcé.");
                    intentions.clear();
                    intentions.add(Intention.STOP);
                    return;
                }
                preciseX += 1.0 * speedFactor * directionFactor;
                position = new Position((int) Math.round(preciseX), position.getY());
                System.out.println("🚗 [" + mode + "] Accélère vers " + position);
            }

            case SLOW_DOWN -> {
                preciseX += 0.5 * speedFactor * directionFactor;
                position = new Position((int) Math.round(preciseX), position.getY());
                System.out.println("🐢 [" + mode + "] Ralentit vers " + position);
            }

            case STOP -> {
                boolean stillNeedsStop = beliefs.contains("FeuRouge", true)
                        && distanceToLight > 0 && distanceToLight < brakingDistance;
                if (stillNeedsStop) {
                    System.out.println("🛑 Maintien à l'arrêt : feu rouge à " + distanceToLight + "m");
                } else {
                    System.out.println("🟢 Reprise après arrêt, situation dégagée → ACCELERATE");
                    intentions.clear();
                    intentions.add(Intention.ACCELERATE);
                }
            }

            case TURN_LEFT, TURN_RIGHT -> {
                boolean toLeft = (intention == Intention.TURN_LEFT);
                Lane targetLane = road.getAdjacentLane(currentLane, toLeft);

                System.out.println("↔️ V" + id + " veut tourner " + (toLeft ? "à gauche" : "à droite"));
                if (targetLane == null) {
                    System.out.println("   ❌ Aucune voie adjacente trouvée");
                    return;
                }

                if (!targetLane.isSameDirection(currentLane)) {
                    System.out.println("   ❌ Voie adjacente n'a pas la même direction");
                    return;
                }

                boolean safe = true;
                for (Vehicle v : targetLane.getVehicles()) {
                    double dx = Math.abs(v.getPreciseX() - this.preciseX);
                    System.out.println("   ↪︎ V" + v.getId() + " à " + dx + "m dans la voie cible");
                    if (dx < 2) {
                        safe = false;
                    }
                }

                if (safe) {
                    System.out.println("   ✅ Changement de voie autorisé");
                    currentLane.removeVehicle(this);
                    currentLane = targetLane;
                    targetLane.addVehicle(this);
                    position = new Position(position.getX(), targetLane.getCenterYInt());
                    this.plan();
                    System.out.println("↔️ V" + id + " a changé " + (toLeft ? "à gauche" : "à droite") + " vers " + targetLane.getId());
                } else {
                    System.out.println("⛔ V" + id + " trop proche d'un véhicule pour changer de voie !");
                }
            }


            case CHANGE_LANE -> {
                int newY = (position.getY() > 0) ? -Math.abs(position.getY()) : Math.abs(position.getY());
                position = new Position(position.getX(), newY);
                System.out.println("➡️ Changement de voie brut vers Y=" + newY);
            }

            case WAIT -> {
                System.out.println("⏸️ Véhicule en attente");
            }
        }
    }


    private void updatePostActionBeliefs() {
        this.previousPosition = new Position(position.getX(), position.getY());
    }

    private double getDistanceToNextLight() {
        return road.getTrafficLights().stream()
                .map(road::getTrafficLightPosition)
                .filter(pos -> pos.getX() > position.getX())
                .mapToDouble(pos -> pos.getX() - position.getX())
                .min().orElse(0.0);
    }

    private double computeDynamicBrakingDistance(Lane lane) {
        double speed = lane.getVehicleSpeed(this);
        return Math.max(5.0, speed / 4.0);
    }

    // === Getters ===
    public Position getPosition() { return position; }
    public double getPreciseX() { return preciseX; }
    public List<Desire> getDesires() { return desires; }
    public Queue<Intention> getIntentions() { return intentions; }
    public List<Intention> getAllIntentions() {
        List<Intention> all = new ArrayList<>(executedIntentions);
        all.addAll(intentions);
        return all;
    }
    public BeliefInitial getBeliefs() { return beliefs; }
    public int getId() { return id; }
    public TransportationMode getMode() { return mode; }
    public int getLaneChangeCount() { return laneChangeCount; }
    public int getFrustrationCount() { return frustrationCount; }
    public double getTravelTimeSeconds() {
        long end = (endTime != null) ? endTime : System.currentTimeMillis();
        return (end - startTime) / 1000.0;
    }
    public List<Position> getPath() {
        return path;
    }
    public int getNextWaypointIndex() {
        return nextWaypointIdx;
    }



}
