package org.example.agent;
import org.example.environment.Environment;
import org.example.environment.Lane;
import org.example.environment.Road;
import org.example.environment.TrafficLight;
import org.example.logic.*;

import java.util.*;

public class Vehicle {
    private BeliefInitial beliefs;
    private List<Desire> desires;
    private List<Goal> goals;
    private Queue<Intention> intentions;
    private Position position;
    private Position destination;
    private Lane currentLane;
    private static final double SAFE_BRAKING_DISTANCE = 20.0;
    private double preciseX;
    private Road road;
    Lane leftLane;
    private static int nextId = 1;
    private final int id;

    public Vehicle(Position position, Position destination) {
        this.id = nextId++;
        this.beliefs = new BeliefInitial();
        this.desires = new ArrayList<>();
        this.goals = new ArrayList<>();
        this.intentions = new LinkedList<>();
        this.position = position;
        this.destination = destination;
        this.preciseX = position.getX();
        initializeGoals();
    }

    public void addDesire(Desire desire) {
        desires.add(desire);
    }
    public void addIntention(Intention intention) {
        intentions.add(intention);
    }

    private void initializeGoals() {
        // Primary goals
        Goal reachDest = new Goal(
                new Desire("REACH_DESTINATION",1),
                new AtomFormula("AtDestination", true)
        );

        Goal avoidCollision = new Goal(
                new Desire("AVOID_COLLISION", 2),
                new NotFormula(new AtomFormula("CollisionRisk", true))
        );

        // Secondary goals
        Goal obeyRules = new Goal(
                new Desire("OBEY_TRAFFIC_RULES", 1),
                new AtomFormula("TrafficRulesObeyed", true)
        );
        Goal avoidJam = new Goal(
                new Desire("AVOID_TRAFFIC_JAM", 2),
                new AndFormula(
                        new NotFormula(new AtomFormula("InTrafficJam", true)),
                        new AtomFormula("Moving", true)
                )
        );

        goals.add(reachDest);
        goals.add(avoidCollision);
        goals.add(obeyRules);
        goals.add(avoidJam);

        // Ajouter les désirs correspondants
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
                position.distanceTo(destination) > 10 &&  // Pas trop près de la destination
                        lane.getVehicleSpeed(this) > 30.0));  // Seuil plus réaliste

        // Autres croyances importantes
        beliefs.addBelief(new Belief("HighSpeed", lane.getVehicleSpeed(this) > 50.0));
        beliefs.addBelief(new Belief("CollisionRisk",
                beliefs.contains("CarAhead", true) && beliefs.contains("HighSpeed", true)));
        if (!intentions.isEmpty()) {
            beliefs.addBelief(new Belief("Moving", true));
        } else {
            beliefs.addBelief(new Belief("Moving", false));
        }
        if (arrived) {
            // Désactiver tous les désirs sauf REACH_DESTINATION
            desires.stream()
                    .filter(d -> !d.getName().equals("REACH_DESTINATION"))
                    .forEach(Desire::achieve);
        }
    }

    private void updateDesires() {
        // Réinitialiser l'état des désirs
        desires.forEach(Desire::reset);

        // Mettre à jour l'état d'accomplissement
        for (Goal goal : goals) {
            if (goal.isAchieved(beliefs)) {
                goal.getDesire().achieve();
            } else {
                // S'assurer que le désir est dans la liste
                if (!desires.contains(goal.getDesire())) {
                    desires.add(goal.getDesire());
                }
            }
        }

        // Trier par priorité
        desires.sort(Comparator.comparingInt(Desire::getPriority));
    }

    private void deliberate() {
        // Trier les désirs par priorité (les non-accomplis en premier)
        List<Desire> activeDesires = desires.stream()
                .filter(d -> !d.isAchieved())
                .sorted(Comparator.comparingInt(Desire::getPriority))
                .toList();

        if (!activeDesires.isEmpty()) {
            generateIntentions(activeDesires.get(0)); // Traiter le désir le plus prioritaire
        }
    }

    private void plan() {
        // Ici on pourrait ajouter une vraie planification
        // Pour l'instant on utilise simplement la file d'intentions
    }

    private void generateIntentions(Desire desire) {
        intentions.clear();

        // Création des formules atomiques de base
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

        // Calcul de la distance au feu le plus proche
        double distanceToLight = getDistanceToNextLight();
        boolean isRedLightNear = beliefs.contains("FeuRouge", true) &&
                distanceToLight < SAFE_BRAKING_DISTANCE;

        // Priorité absolue au feu rouge proche
        if (isRedLightNear) {
            intentions.add(Intention.STOP);
            return; // On ignore les autres désirs si le feu rouge est proche
        }

        switch (desire.getName()) {
            case "REACH_DESTINATION":
                // Règle 1: Si près de la destination → Ralentir
                if (nearDestination.evaluate(beliefs)) {
                    intentions.add(Intention.SLOW_DOWN);
                    break;
                }

                // Règle 2: Si feu vert ET pas de voiture devant ET pas d'obstacle → Accélérer
                LogicalFormula canAccelerate = new AndFormula(
                        feuVert,
                        new AndFormula(
                                new NotFormula(carAhead),
                                new NotFormula(obstacle)
                        )
                );

                // Règle 3: Si obstacle devant → Changer de voie si possible
                LogicalFormula avoidObstacle = new AndFormula(
                        obstacle,
                        new OrFormula(
                                new NotFormula(carLeft),
                                new NotFormula(carRight)
                        )
                );

                // Règle 4: Si voiture devant ET voie libre → Changer de voie
                LogicalFormula canOvertake = new AndFormula(
                        carAhead,
                        new OrFormula(
                                new NotFormula(carLeft),
                                new NotFormula(carRight)
                        )
                );

                // Évaluation hiérarchique des règles
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
                // Règle 1: Risque de collision imminent → Freinage d'urgence
                LogicalFormula emergency = new AndFormula(
                        carAhead,
                        highSpeed
                );

                if (emergency.evaluate(beliefs)) {
                    intentions.add(Intention.STOP);
                }
                break;

            case "OBEY_TRAFFIC_RULES":
                // Règle 1: Feu rouge ou véhicule prioritaire → S'arrêter
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
                // Règle: Embouteillage ET voie alternative libre → Changer de voie
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

        // Comportement par défaut si aucune intention n'a été générée
        if (intentions.isEmpty()) {
            if (beliefs.contains("FeuRouge", true)) {
                intentions.add(Intention.SLOW_DOWN);
            } else {
                intentions.add(Intention.ACCELERATE);
            }
        }
    }

    private boolean isVehicleStoppedAhead(Lane lane) {
        Vehicle ahead = lane.getVehicleAhead(this);
        return ahead != null &&
                (ahead.getIntentions().contains(Intention.STOP) ||
                        ahead.getIntentions().contains(Intention.WAIT));
    }
    private void updatePostActionBeliefs() {
        boolean hasMoved = !position.equals(previousPosition);
        beliefs.addBelief(new Belief("Moving", hasMoved));
        previousPosition = new Position(position.getX(), position.getY());
    }


    private List<Intention> executedIntentions = new ArrayList<>();
    public void act() {
        if (!intentions.isEmpty()) {
            Intention intention = intentions.poll();
            executedIntentions.add(intention); // Garder trace
            executeIntention(intention);
        }
    }

    private void executeIntention(Intention intention) {
        if (currentLane == null) {
            System.out.println("Erreur : Aucune lane définie");
            return;
        }

        if (beliefs.contains("AtDestination", true)) {
            System.out.println("Véhicule arrivé à destination");
            return;
        }

        // Vérification constante du feu rouge
        double distanceToLight = getDistanceToNextLight();
        boolean isRedLightNear = beliefs.contains("FeuRouge", true) &&
                distanceToLight < SAFE_BRAKING_DISTANCE;

        // Si feu rouge proche, forcer l'arrêt
        if (isRedLightNear && intention != Intention.STOP) {
            intentions.clear();
            intentions.add(Intention.STOP);
            return;
        }

        int directionFactor = (currentLane.getDirection() == Lane.DIRECTION_LEFT) ? -1 : 1;

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
        plan();

        // 5. Exécution
        act();

        // Mise à jour des croyances post-action
        updatePostActionBeliefs();
    }
    private double getDistanceToNextLight() {
        // Trouver le feu le plus proche devant le véhicule
        for (TrafficLight light : road.getTrafficLights()) {
            double lightX = 30; // Position du feu (devrait être stockée dans TrafficLight)
            if (lightX > position.getX()) { // Feu devant le véhicule
                return lightX - position.getX();
            }
        }
        return Double.MAX_VALUE; // Aucun feu devant
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

}
