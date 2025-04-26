package org.example.agent;

import javafx.scene.paint.Color;
import org.example.environment.*;
import org.example.logic.*;
import org.example.planning.DijkstraAlgorithm;
import org.example.planning.Graph;
import org.example.planning.GraphNode;

import java.util.*;
import java.util.List;

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
    private boolean changingDirection = false; // Est-ce qu'on est en train de monter/descendre
    private int verticalDirection = 0; // 1 = monter (Y+), -1 = descendre (Y-)


    private BeliefInitial beliefs;
    private List<Desire> desires;
    private List<Goal> goals;
    private Queue<Intention> intentions;
    private List<Intention> executedIntentions = new ArrayList<>();
    private static final double EARLY_SLOWDOWN_DISTANCE = 15.0; // mètres


    private List<Position> path = new ArrayList<>();
    private int nextWaypointIdx = 0;
    private Position lastPlannedPosition = null;

    private Environment environment;
    private boolean useGlobalGraph = true;
    private long lastPlanTime = 0;
    private static final long PLAN_COOLDOWN_MS = 1000;
    private long lastLaneChangeTime = 0;
    private static final long LANE_CHANGE_COOLDOWN_MS = 100;
    private Color pathColor = Color.ORANGE;

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

        boolean obstacleAhead = lane.getObstacles().stream()
                .anyMatch(obs -> obs.getPosition().getX() > position.getX() &&
                        obs.getPosition().getX() - position.getX() < 10);
        beliefs.addBelief(new Belief("ObstacleAhead", obstacleAhead));

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

        long now = System.currentTimeMillis();

        // Seulement replanifier dans 2 cas : obstacle ou chemin terminé
        boolean mustReplan = beliefs.contains("ObstacleAhead", true)
                || (path == null || path.isEmpty())
                || (nextWaypointIdx >= path.size());

        if (mustReplan && (now - lastPlanTime > PLAN_COOLDOWN_MS)) {
            plan();
            lastPlanTime = now;
        }
    }


    public void plan() {
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

        if (path == null || path.isEmpty()) {
            System.out.println("❌ Aucun chemin trouvé pour V" + id + " !");
            return;  // ❗ Ne pas assigner un chemin vide
        }

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

        boolean obstacleDetected = beliefs.contains("ObstacleAhead", true);

        // --- Priorité 1 : Urgences d'abord (feu rouge etc.) ---
        LogicalFormula feuRouge = new AtomFormula("FeuRouge", true);
        LogicalFormula feuOrange = new AtomFormula("FeuOrange", true);
        LogicalFormula carAhead = new AtomFormula("CarAhead", true);
        LogicalFormula carLeft = new AtomFormula("CarOnLeft", true);
        LogicalFormula carRight = new AtomFormula("CarOnRight", true);
        LogicalFormula highSpeed = new AtomFormula("HighSpeed", true);
        LogicalFormula priorityVehicle = new AtomFormula("PriorityVehicle", true);

        if (feuRouge.evaluate(beliefs) && beliefs.contains("FeuDevant", true) && !beliefs.contains("FeuFranchi", true)
                && distanceToLight > 0 && distanceToLight < brakingDistance + 5) {
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

        if (new AndFormula(carAhead, highSpeed).evaluate(beliefs)) {
            tempIntentions.put(Intention.STOP, 0);
        }

        if (new OrFormula(feuRouge, priorityVehicle).evaluate(beliefs)
                && distanceToLight > 0 && distanceToLight < 25) {
            tempIntentions.put(Intention.SLOW_DOWN, 0);
        }

        Vehicle vehicleAhead = currentLane.getVehicleAhead(this);
        if (vehicleAhead != null) {
            double distanceToCarAhead = vehicleAhead.getPreciseX() - this.preciseX;

            if (distanceToCarAhead < EARLY_SLOWDOWN_DISTANCE) {
                System.out.println("⚠️ Ralentir : Véhicule détecté à " + distanceToCarAhead + "m devant");
                if (distanceToCarAhead < 5) {
                    tempIntentions.put(Intention.STOP, -5); // collision imminent
                } else {
                    tempIntentions.put(Intention.SLOW_DOWN, -3); // ralentir plus tôt
                }
            }
        }


        // --- Priorité 2 : Suivre le chemin Dijkstra
        if (path != null && !path.isEmpty() && nextWaypointIdx < path.size()) {
            Position target = path.get(nextWaypointIdx);
            double distanceToTarget = position.distanceTo(target);

            double threshold = Math.max(0.5, currentLane.getVehicleSpeed(this) * 0.05);

            if (distanceToTarget < threshold) {
                nextWaypointIdx = Math.min(nextWaypointIdx + 1, path.size() - 1);
                target = path.get(nextWaypointIdx);
            }

            double dx = target.getX() - position.getX();
            double dy = target.getY() - position.getY();

            if (Math.abs(dy) > 1.0 && distanceToTarget < 2.0) {
                boolean canChangeLane = (System.currentTimeMillis() - lastLaneChangeTime > 1000); // cooldown

                if (canChangeLane) {  // ✅ NOUVEAU : vérifie vraiment cooldown AVANT d'ajouter TURN
                    if (dy > 0 && road.hasRightLane(currentLane)) {
                        tempIntentions.put(Intention.TURN_RIGHT, -2);
                    } else if (dy < 0 && road.hasLeftLane(currentLane)) {
                        tempIntentions.put(Intention.TURN_LEFT, -2);
                    }
                } else {
                    System.out.println("⏳ Cooldown changement de voie pas encore fini, pas de TURN proposé");
                }
            }
        }


        // --- Priorité 3 : Gestion obstacle
        if (obstacleDetected) {
            // ⚠️ Tenter de changer de voie plutôt que de stopper
            boolean canLeft = road.hasLeftLane(currentLane) && !beliefs.contains("CarOnLeft", true);
            boolean canRight = road.hasRightLane(currentLane) && !beliefs.contains("CarOnRight", true);

            if (canLeft) {
                tempIntentions.put(Intention.TURN_LEFT, 1);
            } else if (canRight) {
                tempIntentions.put(Intention.TURN_RIGHT, 1);
            } else {
                tempIntentions.put(Intention.STOP, 2); // Si impossible -> STOP
            }
        }

        // --- Fallback
        if (tempIntentions.isEmpty()) {
            tempIntentions.put(Intention.FOLLOW_PATH, 5);
        }

        tempIntentions.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .forEach(intentions::add);

        System.out.println("🎯 Intentions finales V" + id + " = " + intentions);
    }





    public void act() {
        // Chercher le prochain target du chemin

        if (!intentions.isEmpty()) {
            Intention intention = intentions.poll();
            executedIntentions.add(intention);

            Position target = (path != null && nextWaypointIdx < path.size()) ? path.get(nextWaypointIdx) : null;
            executeIntention(intention, target);
        }
    }

    private void executeIntention(Intention intention, Position target) {
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
                moveTowardsTarget(target,1.0);
                System.out.println("🚗 [" + mode + "] Accélère vers " + position);
            }

            case SLOW_DOWN -> {
                moveTowardsTarget(target, 0.5);
                System.out.println("🐢 [" + mode + "] Ralentit vers " + position);
            }

            case STOP -> {
                boolean stillNeedsStop = false;

                Vehicle vehicleAhead = currentLane.getVehicleAhead(this);
                if (vehicleAhead != null) {
                    double distanceToCarAhead = vehicleAhead.getPreciseX() - this.preciseX;
                    if (distanceToCarAhead < computeDynamicBrakingDistance(currentLane)) {
                        stillNeedsStop = true;
                    }
                }

                if (beliefs.contains("FeuRouge", true) && distanceToLight > 0 && distanceToLight < brakingDistance) {
                    stillNeedsStop = true;
                }

                if (stillNeedsStop) {
                    System.out.println("🛑 V" + id + " reste arrêté pour sécurité");
                    // Ne bouge pas ! Véhicule bloqué
                } else {
                    System.out.println("🟢 V" + id + " peut repartir, relance ACCELERATE");
                    intentions.clear();
                    executeIntention(Intention.ACCELERATE, target);
                }
            }
            case FOLLOW_PATH -> {
                moveTowardsTarget(path.get(nextWaypointIdx), 1.0);
                System.out.println("🛤️ V" + id + " suit son chemin Dijkstra...");
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
    public void setPathColor(Color color) {
        this.pathColor = color;
    }

    public Color getPathColor() {
        return pathColor;
    }
    private void moveTowardsTarget(Position target, double speedMultiplier) {
        if (changingDirection) {
            // Mouvement vertical pur
            double stepY = speedMultiplier * speedFactor * 1.5 * verticalDirection;
            position = new Position(position.getX(), position.getY() + (int) Math.round(stepY));

            // Lorsque suffisamment monté/descendu, on arrête
            if (Math.abs(position.getY() - target.getY()) < 1) {
                changingDirection = false;
                verticalDirection = 0;
                System.out.println("✅ Changement de direction terminé, reprend mouvement normal.");
            }
            return; // On ne bouge pas en X pendant qu'on monte
        }

        if (target == null) {
            int directionFactor = (currentLane.getDirection() == Lane.DIRECTION_LEFT) ? -1 : 1;
            preciseX += speedMultiplier * speedFactor * directionFactor;
            position = new Position((int) Math.round(preciseX), position.getY());
            System.out.println("🚗 Avance simple vers " + position);
            return;
        }

        double dx = target.getX() - position.getX();
        double dy = target.getY() - position.getY();

        // --- TOLERANCE pour éviter de changer de voie tout le temps
        double laneChangeTolerance = 0.5;
        long now = System.currentTimeMillis();

        if (Math.abs(dy) > laneChangeTolerance) {
            // --- Vérifier cooldown changement de voie
            if (now - lastLaneChangeTime > 1000) {  // ✅ 1 seconde de délai minimal entre 2 changements
                int targetY = target.getY();
                if (Math.abs(position.getY() - targetY) > laneChangeTolerance) {
                    position = new Position(position.getX(), targetY);
                    lastLaneChangeTime = now; // ✅ update cooldown timer
                    System.out.println("↔️ Changement de voie pour suivre Y = " + targetY);
                    return; // ⚡ Change Y d'abord puis avance X au prochain cycle
                }
            } else {
                System.out.println("⏳ Attente cooldown avant nouveau changement de voie...");
            }
        }

        // --- Puis avancer vers le waypoint en X
        if (Math.abs(dx) > 0.1) {
            int directionFactor = (currentLane.getDirection() == Lane.DIRECTION_LEFT) ? -1 : 1;
            double step = speedMultiplier * speedFactor * 1.5 * Math.signum(dx);

            preciseX += step;
            if (Math.abs(preciseX - position.getX()) >= 0.5) {
                position = new Position((int) Math.round(preciseX), position.getY());
            }

            System.out.println((step > 0 ? "🚗" : "⬅️") + " Avance vers waypoint " + target);
        }
    }
    // Dans ta classe Vehicle :

    public Road getRoad() {
        return road;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public void setPreciseX(double preciseX) {
        this.preciseX = preciseX;
    }
    public void setRoad(Road road) {
        this.road = road;
    }
    public void setChangingDirection(boolean changing) {
        this.changingDirection = changing;
    }

    public void setVerticalDirection(int dir) {
        this.verticalDirection = dir;
    }

    public boolean isChangingDirection() {
        return changingDirection;
    }








}