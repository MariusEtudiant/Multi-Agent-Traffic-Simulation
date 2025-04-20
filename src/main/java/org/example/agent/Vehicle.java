package org.example.agent;


/*
The heart of the reactor !! this class models an intelligent vehicle capable of navigating in a simulated traffic environment by making
decisions based on its perceptions.
Architecture BDI: BeliefInitial, stores perceptions (lights, obstacles, other vehicles), List<Desire> Hierarchical goals (reach destination, avoid collisions)
Intentions Queue<Intention> Concrete actions to execute (accelerate, turn, etc.)

perceivedEnvironment() méthod is not a redundancy of BeliefsInitial's updateBeliefs, even if it did create beliefs: in fact, it coordinates
the overall perception of the vehicle and updates certain specific beliefs that require calculations or contexts specific to the vehicle.

exemple:
Scenario: A red light in front of the vehicle
updateBeliefs (in BeliefInitial):

Detects: RedLight = true (via lane.isTrafficLightGreen())

Detects: CarAhead = false

perceivedEnvironment (in Vehicle):

Calculates: CollisionRisk = false (because no car in front)

Check: NearDestination = false (via distance calculation)
 */

import org.example.environment.Environment;
import org.example.environment.Lane;
import org.example.environment.Road;
import org.example.environment.TrafficLight;
import org.example.logic.*;
import org.example.planning.DijkstraAlgorithm;
import org.example.planning.Graph;
import org.example.planning.GraphNode;

import java.util.*;

public class Vehicle {
    //bdi
    private BeliefInitial beliefs;
    private List<Desire> desires;
    private List<Goal> goals;
    private Queue<Intention> intentions;

    //positions
    private Position position;
    private Position destination;
    private Lane currentLane;
    private static final double SAFE_BRAKING_DISTANCE = 20.0;
    private double preciseX;
    private Road road;
    private static int nextId = 1;
    private final int id;

    //plan
    private List<Position> path = new ArrayList<>();
    private Position lastPlannedPosition = null;
    private Environment environment;
    private boolean useGlobalGraph = true; // par défaut on active
    private int nextWaypointIdx = 0;
    private long lastPlanTime = 0;
    private static final long PLAN_COOLDOWN_MS = 1000;
    private long lastLaneChangeTime = 0;
    private static final long LANE_CHANGE_COOLDOWN_MS = 1000; // 1 seconde





    //metrics
    private long startTime;
    private int laneChangeCount = 0;
    private int frustrationCount = 0;
    private Long endTime = null;

    public Vehicle(Position position, Position destination, Environment environment) {
        this.id = nextId++;
        this.beliefs = new BeliefInitial();
        this.desires = new ArrayList<>();
        this.goals = new ArrayList<>();
        this.intentions = new LinkedList<>();
        this.position = position;
        this.destination = destination;
        this.environment = environment;
        this.preciseX = position.getX();
        this.startTime = System.currentTimeMillis();
        initializeGoals();
    }

    public double getTravelTimeSeconds() {
        long end = endTime != null ? endTime : System.currentTimeMillis();
        return (end - startTime) / 1000.0;
    }
    public void setUseGlobalGraph(boolean useGlobalGraph) {
        this.useGlobalGraph = useGlobalGraph;
    }
    public int getLaneChangeCount() {
        return laneChangeCount;
    }

    public int getFrustrationCount() {
        return frustrationCount;
    }

    private void initializeGoals() {
        //primary goals
        Goal reachDest = new Goal(
                new Desire("REACH_DESTINATION",1),
                new AtomFormula("AtDestination", true)
        );
        Goal obeyRules = new Goal(
                new Desire("OBEY_TRAFFIC_RULES", 1),
                new AtomFormula("TrafficRulesObeyed", true)
        );
        //secondary goals

        Goal avoidCollision = new Goal(
                new Desire("AVOID_COLLISION", 2),
                new NotFormula(new AtomFormula("CollisionRisk", true))
        );

        Goal avoidJam = new Goal(
                new Desire("AVOID_TRAFFIC_JAM", 2),
                        new NotFormula(new AtomFormula("InTrafficJam", true))
        );

        goals.add(reachDest);
        goals.add(avoidCollision);
        goals.add(obeyRules);
        goals.add(avoidJam);

        desires.add(reachDest.getDesire());
        desires.add(avoidCollision.getDesire());
        desires.add(obeyRules.getDesire());
        desires.add(avoidJam.getDesire());
    }


    public void perceivedEnvironment(Lane lane, Road road){
        beliefs.updateBeliefs(lane,road,this);

        double distanceToDest = position.distanceTo(destination);
        boolean arrived = distanceToDest < 5.0;

        beliefs.addBelief(new Belief("AtDestination", distanceToDest < 5.0));
        beliefs.addBelief(new Belief("NearDestination", distanceToDest < 15.0));
        beliefs.addBelief(new Belief("HighSpeed",
                position.distanceTo(destination) > 10 &&
                        lane.getVehicleSpeed(this) > 30.0));
        beliefs.addBelief(new Belief("CollisionRisk",
                beliefs.contains("CarAhead", true) && beliefs.contains("HighSpeed", true)));


        if (arrived) {
            //only keep reach_destination
            desires.stream()
                    .filter(d -> !d.getName().equals("REACH_DESTINATION"))
                    .forEach(Desire::achieve);
        }
    }

    private void updateDesires() {
        //res
        desires.forEach(Desire::reset);

        //update achievements state
        for (Goal goal : goals) {
            if (goal.isAchieved(beliefs)) {
                goal.getDesire().achieve();
            } else {
                if (!desires.contains(goal.getDesire())) {
                    desires.add(goal.getDesire());
                }
            }
        }

        //priority sort
        desires.sort(Comparator.comparingInt(Desire::getPriority));
    }

    private void deliberate() {
        //non-accomplish at first
        List<Desire> activeDesires = desires.stream()
                .filter(d -> !d.isAchieved())
                .sorted(Comparator.comparingInt(Desire::getPriority))
                .toList();

        if (!activeDesires.isEmpty()) {
            generateIntentions(activeDesires.get(0));
        }
    }

    private void plan() {
        if (beliefs.contains("AtDestination", true)) {
            System.out.println("✅ Véhicule " + this.id + " : déjà à destination.");
            return;
        }

        Graph roadGraph;
        if (useGlobalGraph && environment != null) {
            roadGraph = environment.getGlobalGraph();
            System.out.println("🌐 Véhicule " + this.id + " : utilisation du graphe GLOBAL");
        } else {
            roadGraph = road.getGraph();
            System.out.println("🛣️ Véhicule " + this.id + " : utilisation du graphe LOCAL");
        }
        if (roadGraph == null) {
            System.out.println("❌ Aucun graphe de route n’est disponible.");
            return;
        }

        // ⚠️ Snap X uniquement, mais garder Y = destination.getY()
        int gridSize = 10;
        int snappedStartX = Math.round((float) position.getX() / gridSize) * gridSize;
        int startY       = currentLane.getCenterYInt();
        Position snappedStart = new Position(snappedStartX, startY);

        int snappedGoalX = Math.round((float) destination.getX() / gridSize) * gridSize;
        // On remplace startY par la Y réelle de la destination
        Position snappedGoal  = new Position(snappedGoalX, destination.getY());

        GraphNode startNode = roadGraph.getNode(snappedStart);
        GraphNode goalNode  = roadGraph.getNode(snappedGoal);

        if (startNode == null || goalNode == null) {
            System.out.println("❌ Position de départ ou d'arrivée non trouvée dans le graphe !");
            System.out.println("   Start: " + snappedStart + " → " + startNode);
            System.out.println("   Goal : " + snappedGoal  + " → " + goalNode);
            return;
        }

        this.path = DijkstraAlgorithm.computePath(roadGraph, snappedStart, snappedGoal);

        if (path.isEmpty()) {
            System.out.println("⚠️ Aucune route trouvée pour le véhicule " + this.id);
        } else {
            System.out.println("🚗 Véhicule " + this.id + " → Chemin calculé : " + path);
        }
        this.nextWaypointIdx = 0;
        this.lastPlannedPosition = this.position;
    }


    public void planIfNeeded() {
        if (beliefs.contains("AtDestination", true)) return;

        boolean obstacleAhead = beliefs.contains("ObstacleAhead", true);
        boolean trafficJam = beliefs.contains("InTrafficJam", true);
        boolean hasMovedFar = lastPlannedPosition != null &&
                lastPlannedPosition.distanceTo(position) > 9.0;
        boolean shouldPlan = obstacleAhead || trafficJam || path.isEmpty() || hasMovedFar;

        long now = System.currentTimeMillis();
        boolean cooldownPassed = (now - lastPlanTime > PLAN_COOLDOWN_MS);

        if (shouldPlan && cooldownPassed) {
            System.out.println("📍 [V" + id + "] Recalcul du chemin déclenché");
            plan();
            lastPlannedPosition = position;
            lastPlanTime = now;
        }
    }




    private void generateIntentions(Desire desire) {
        intentions.clear();

        //Atom basics formulas
        LogicalFormula feuVert = new AtomFormula("FeuVert", true);
        LogicalFormula feuRouge = new AtomFormula("FeuRouge", true);
        LogicalFormula carAhead = new AtomFormula("CarAhead", true);
        LogicalFormula carLeft = new AtomFormula("CarOnLeft", true);
        LogicalFormula carRight = new AtomFormula("CarOnRight", true);
        LogicalFormula obstacle = new AtomFormula("ObstacleAhead", true);
        LogicalFormula inTrafficJam = new AtomFormula("InTrafficJam", true);
        LogicalFormula priorityVehicle = new AtomFormula("PriorityVehicle", true);
        LogicalFormula nearDestination = new AtomFormula("NearDestination", true);
        LogicalFormula highSpeed = new AtomFormula("HighSpeed", true);

        double distanceToLight = getDistanceToNextLight(); //distance of the nextLight
        boolean isRedLightNear = beliefs.contains("FeuRouge", true) &&
                distanceToLight < SAFE_BRAKING_DISTANCE;
        boolean isOrangeLightNear = beliefs.contains("FeuOrange", true) &&
                distanceToLight < SAFE_BRAKING_DISTANCE / 1.5;


        if (isRedLightNear || isOrangeLightNear) {
            intentions.add(Intention.SLOW_DOWN); // ou STOP si encore plus proche
            return;
        }
        if (distanceToLight < 5) {
            intentions.add(Intention.STOP);
        } else if (distanceToLight < 15) {
            intentions.add(Intention.SLOW_DOWN);
        }

        switch (desire.getName()) {
            case "REACH_DESTINATION":
                //rule If near destination  => SLOW_DOWN
                if (nearDestination.evaluate(beliefs)) {
                    intentions.add(Intention.SLOW_DOWN);
                    break;
                }

                //rule If light green and not carAhead and not obstacleAhead => Accelerate
                LogicalFormula canAccelerate = new AndFormula(
                        feuVert,
                        new AndFormula(
                                new NotFormula(carAhead),
                                new NotFormula(obstacle)
                        )
                );

                //rule: If obstacleAhead => change lane if possible
                LogicalFormula avoidObstacle = new AndFormula(
                        obstacle,
                        new OrFormula(
                                new NotFormula(carLeft),
                                new NotFormula(carRight)
                        )
                );

                //rule: if carAheaddevant and free Lane => Change lane
                LogicalFormula canOvertake = new AndFormula(
                        carAhead,
                        new OrFormula(
                                new NotFormula(carLeft),
                                new NotFormula(carRight)
                        )
                );

                //Hierarchical evaluation of rules
                if (canAccelerate.evaluate(beliefs)) {
                    intentions.add(Intention.ACCELERATE);
                }
                else if (avoidObstacle.evaluate(beliefs) || canOvertake.evaluate(beliefs)) {
                    boolean canTurnLeft = !beliefs.contains("CarOnLeft", true) && road.hasLeftLane(currentLane);
                    boolean canTurnRight = !beliefs.contains("CarOnRight", true) && road.hasRightLane(currentLane);

                    if (canTurnLeft) {
                        intentions.add(Intention.TURN_LEFT);
                    }
                    else if (canTurnRight) {
                        intentions.add(Intention.TURN_RIGHT);
                    }
                    else {
                        intentions.add(Intention.SLOW_DOWN);
                    }
                }
                break;

            case "AVOID_COLLISION":
                //rule: Imminent risk of collision => Emergency braking
                LogicalFormula emergency = new AndFormula(
                        carAhead,
                        highSpeed
                );

                if (emergency.evaluate(beliefs)) {
                    intentions.add(Intention.STOP);
                }
                break;

            case "OBEY_TRAFFIC_RULES":
                //rule: Red light or priority vehicle => Stop
                LogicalFormula mustStop = new OrFormula(feuRouge, priorityVehicle);
                if (mustStop.evaluate(beliefs)) {
                    double distToLight = getDistanceToNextLight();

                    if (distToLight < SAFE_BRAKING_DISTANCE / 2) {
                        intentions.add(Intention.STOP);
                    } else if (distToLight < SAFE_BRAKING_DISTANCE) {
                        intentions.add(Intention.SLOW_DOWN);
                    }
                }
                break;

            case "AVOID_TRAFFIC_JAM":
                //rule: Traffic jam AND free alternative lane → Change lane
                LogicalFormula changeLane = new AndFormula(
                        inTrafficJam,
                        new OrFormula(
                                new NotFormula(carLeft),
                                new NotFormula(carRight)
                        )
                );

                if (changeLane.evaluate(beliefs)) {
                    intentions.add(Intention.CHANGE_LANE);
                }
                break;
        }

        // =================== ⬇️ FOLLOW THE PLAN if no higher priority triggered
        if (!path.isEmpty() && nextWaypointIdx < path.size()) {
            Position target = path.get(nextWaypointIdx);

            if (position.distanceTo(target) < 2.0) {
                nextWaypointIdx++;
                if (nextWaypointIdx >= path.size()) {
                    System.out.println("🟢 Véhicule " + id + " a atteint la fin du chemin planifié");
                    return;
                }
                target = path.get(nextWaypointIdx);
            }

            double dx = target.getX() - position.getX();
            double dy = target.getY() - position.getY();

            if (Math.abs(dx) > Math.abs(dy)) {
                if (dx > 0) {
                    intentions.add(Intention.ACCELERATE);
                } else {
                    intentions.add(Intention.SLOW_DOWN);
                }
            } else {
                long now = System.currentTimeMillis();
                boolean canChangeLane = (now - lastLaneChangeTime > LANE_CHANGE_COOLDOWN_MS);

                // Si trop de différence de Y → probablement erreur de snapping → on ignore
                if (Math.abs(dy) > 20) {
                    System.out.println("⛔ Ignoré waypoint trop éloigné en Y : " + target);
                    intentions.add(Intention.WAIT);
                }
                else if (dy > 0 && road.hasRightLane(currentLane) && canChangeLane) {
                    intentions.add(Intention.TURN_RIGHT);
                    lastLaneChangeTime = now;
                }
                else if (dy < 0 && road.hasLeftLane(currentLane) && canChangeLane) {
                    intentions.add(Intention.TURN_LEFT);
                    lastLaneChangeTime = now;
                }
                else {
                    intentions.add(Intention.WAIT);
                }
            }



            System.out.println("🧭 Véhicule " + id + " suit plan vers " + target);
            return;
        }


        //default behavior
        if (intentions.isEmpty()) {
            if (beliefs.contains("FeuRouge", true)) {
                intentions.add(Intention.SLOW_DOWN);
            } else {
                intentions.add(Intention.ACCELERATE);
            }
        }
    }

    private void updatePostActionBeliefs() {
        previousPosition = new Position(position.getX(), position.getY());
    }

    private List<Intention> executedIntentions = new ArrayList<>();
    public void act() {
        if (!intentions.isEmpty()) {
            Intention intention = intentions.poll();
            executedIntentions.add(intention); // Garder trace
            executeIntention(intention);
            System.out.println("🎯 Véhicule " + id + " intention exécutée: " + intention);
        }
    }

    private void executeIntention(Intention intention) {
        if (currentLane == null) {
            System.out.println("Erreur : Aucune lane définie");
            return;
        }

        if (beliefs.contains("AtDestination", true)) {
            if(endTime == null){
                endTime = System.currentTimeMillis();
            }
            System.out.println("Véhicule arrivé à destination");
            return;
        }

        Road.RoadCondition condition = road.getCondition();
        double distanceToLight = getDistanceToNextLight();
        boolean isRedLightNear = beliefs.contains("FeuRouge", true) &&
                distanceToLight < SAFE_BRAKING_DISTANCE;

        if (isRedLightNear && intention != Intention.STOP) {
            intentions.clear();
            intentions.add(Intention.STOP);
            return;
        }

        int directionFactor = (currentLane.getDirection() == Lane.DIRECTION_LEFT) ? -1 : 1;

        if (intention == Intention.TURN_LEFT || intention == Intention.TURN_RIGHT || intention == Intention.CHANGE_LANE) {
            laneChangeCount++;
        }

        if (beliefs.contains("InTrafficJam", true) || beliefs.contains("ObstacleAhead", true)) {
            frustrationCount++;
        }

        switch (intention) {
            case ACCELERATE:
                if (!isRedLightNear) {
                    preciseX += 1.0 * directionFactor;
                    position = new Position((int) Math.round(preciseX), position.getY());
                    System.out.println("Accélération vers " + position);
                }
                break;

            case SLOW_DOWN:
                preciseX += 0.5 * directionFactor;
                position = new Position((int) Math.round(preciseX), position.getY());
                System.out.println("Ralentissement vers " + position);
                break;

            case TURN_LEFT:
                Lane leftLane = road.getLeftLane(currentLane);
                if (leftLane != null && leftLane.isSameDirection(currentLane)) {
                    currentLane.removeVehicle(this);
                    currentLane = leftLane;
                    leftLane.addVehicle(this);
                    position = new Position(position.getX(), leftLane.getCenterYInt());
                    System.out.println("Changement à gauche vers " + leftLane.getId());
                }
                break;

            case TURN_RIGHT:
                Lane rightLane = road.getRightLane(currentLane);
                if (rightLane != null && rightLane.isSameDirection(currentLane)) {
                    currentLane.removeVehicle(this);
                    currentLane = rightLane;
                    rightLane.addVehicle(this);
                    position = new Position(position.getX(), rightLane.getCenterYInt());
                    System.out.println("Changement à droite vers " + rightLane.getId());
                }
                break;

            case CHANGE_LANE:
                // Implémentation plus réaliste du changement de voie
                int newY = (position.getY() > 0) ?
                        -Math.abs(position.getY()) :
                        Math.abs(position.getY());
                position = new Position(position.getX(), newY);
                System.out.println("Changement de voie vers " + position);
                break;

            case STOP:
                // Vérifier si le feu est toujours rouge avant de rester arrêté
                if (beliefs.contains("FeuRouge", true) && distanceToLight < SAFE_BRAKING_DISTANCE) {
                    System.out.println("Véhicule arrêté au feu rouge");
                } else {
                    // Si le feu est vert ou loin, repartir
                    intentions.add(Intention.ACCELERATE);
                }
                break;

            case WAIT:
                System.out.println("Véhicule en attente");
                break;
        }
    }
    private Position previousPosition;

    public void bdiCycle(Lane lane, Road road) {
        this.currentLane = lane;
        this.road = road;
        previousPosition = new Position(position.getX(), position.getY());
        // 1. Perception - Mise à jour des croyances
        perceivedEnvironment(lane, road);

        // 2. Mise à jour des désirs
        updateDesires();

        // 3. Délibération
        deliberate();

        // 4. Planification (simplifiée)
        planIfNeeded();

        // 5. Exécution
        act();

        // Mise à jour des croyances post-action
        updatePostActionBeliefs();
    }

    // gets
    private double getDistanceToNextLight() {
        double minDistance = Double.MAX_VALUE;

        for (TrafficLight light : road.getTrafficLights()) {
            Position lightPos = road.getTrafficLightPosition(light);

            if (lightPos != null && lightPos.getX() > position.getX()) {
                double distance = lightPos.getX() - position.getX();
                minDistance = Math.min(minDistance, distance);
            }
        }

        return minDistance;
    }



    public Position getPosition() {
        return position;
    }
    public List<Desire> getDesires() {
        return desires;
    }
    public Queue<Intention> getIntentions() {
        return intentions;
    }
    public List<Intention> getAllIntentions() {
        List<Intention> all = new ArrayList<>();
        all.addAll(executedIntentions);
        all.addAll(intentions);
        return all;
    }
    public BeliefInitial getBeliefs() {return beliefs;}
    public int getId() {
        return this.id;
    }

    //no use but maybe later:

    private boolean isVehicleStoppedAhead(Lane lane) {
        Vehicle ahead = lane.getVehicleAhead(this);
        return ahead != null &&
                (ahead.getIntentions().contains(Intention.STOP) ||
                        ahead.getIntentions().contains(Intention.WAIT));
    }

}
