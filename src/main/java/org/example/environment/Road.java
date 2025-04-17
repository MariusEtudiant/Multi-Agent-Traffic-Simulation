package org.example.environment;

import org.example.agent.Vehicle;
import org.example.agent.Position;
import java.util.ArrayList;
import java.util.List;
/**
 * Represents a road in the simulation environment.
 * Can contain vehicles, manage their positioning and provide metrics.
 */
public class Road {
    private final String id;
    private final double length;
    private static final int maxCapacity = 40;
    private final List<Position> entryPoints;  // entry points/end (intersections etc)
    private final List<TrafficLight> trafficLights;
    private final List<Lane> lanes;
    private boolean isCongested;
    private RoadCondition condition = RoadCondition.DRY;
    private static final double APPROACH_DISTANCE = 50.0;
    private static final double CONGESTION_THRESHOLD = 0.8;


    // MDP control parameters
    private boolean useMDP = true;
    private int mdpDecisionInterval = 5;
    private int stepCounter = 0;

    // Construct
    public Road(String id, double length, List<Position> entryPoints) {
        this.id = id;
        this.length = length;
        this.entryPoints = entryPoints;
        this.isCongested = false;
        this.trafficLights = new ArrayList<>();
        this.lanes = new ArrayList<>();
    }
    // for the next semester
    public enum RoadCondition{
        DRY(1.0), WET(0.7), ICY(0.3);

        private final double frictionFactor;

        RoadCondition(double frictionFactor) {
            this.frictionFactor = frictionFactor;
        }

        public double getFrictionFactor() {
            return frictionFactor;
        }
    }

    public void setCondition(RoadCondition condition) {
        this.condition = condition;
    }

    public RoadCondition getCondition() {
        return condition;
    }

    public void addLane(Lane lane) {
        lanes.add(lane);
        lane.setRoad(this);
    }
    public static int maxCapacityCount(){return maxCapacity;}
    public void addTrafficLight(TrafficLight trafficLight, Position position){
        trafficLights.add(trafficLight);
    }

    public boolean hasLeftLane(Lane currentLane) {
        int index = lanes.indexOf(currentLane);
        return index > 0;
    }
    public boolean hasRightLane(Lane currentLane) {
        int index = lanes.indexOf(currentLane);
        return index < lanes.size() - 1;
    }
    public boolean isCongested() {
        return isCongested;
    }

    public Lane getRightLane(Lane currentLane) {
        if (!hasRightLane(currentLane)) return null;
        return lanes.get(lanes.indexOf(currentLane) + 1);
    }
    public Lane getLeftLane(Lane currentLane) {
        if (!hasLeftLane(currentLane)) return null;
        return lanes.get(lanes.indexOf(currentLane) - 1);
    }
    public String getId() {
        return id;
    }
    public int getMaxCapacity() {
        return maxCapacity;
    }
    public List<TrafficLight> getTrafficLights() {
        return trafficLights;
    }

    private void coordinateTrafficLights() {
        if (trafficLights.size() <= 1) return;

        // Trouver le feu avec le plus de trafic
        TrafficLight busiestLight = null;
        int maxTraffic = -1;

        for (TrafficLight light : trafficLights) {
            int trafficValue = light.getCurrentTraffic().ordinal();
            if (trafficValue > maxTraffic) {
                maxTraffic = trafficValue;
                busiestLight = light;
            }
        }

        // Mettre le feu le plus chargé au vert, les autres au rouge
        if (busiestLight != null) {
            for (TrafficLight light : trafficLights) {
                if (light == busiestLight) {
                    if (light.getState() != TrafficLight.LightColor.GREEN) {
                        light.executeAction("SWITCH_GREEN");
                    }
                } else {
                    if (light.getState() != TrafficLight.LightColor.RED) {
                        light.executeAction("SWITCH_RED");
                    }
                }
            }
        }
    }

    private boolean checkCongestion() {
        final double CONGESTION_THRESHOLD = 0.8;
        int totalVehicles = lanes.stream().mapToInt(Lane::getVehicleCount).sum();
        return totalVehicles > maxCapacity * CONGESTION_THRESHOLD;
    }

    // New methods for MDP control
    public void enableMDP(boolean enable) {
        this.useMDP = enable;
    }

    public void setMDPDecisionInterval(int interval) {
        this.mdpDecisionInterval = interval;
    }

    public void trainTrafficLights() {
        for (TrafficLight light : trafficLights) {
            light.printTransitionMatrix();
            light.valueIteration(0.01);
            light.printPolicy();
        }
    }

    public int countVehiclesApproaching(TrafficLight light) {
        final double APPROACH_DISTANCE = 50.0; // constante pour la distance
        Position lightPosition = getLightPosition(light.getId());

        return (int) lanes.stream()
                .flatMap(lane -> lane.getVehicles().stream())
                .filter(v -> v.getPosition().distanceTo(lightPosition) < APPROACH_DISTANCE)
                .count();
    }

    private Position getLightPosition(String lightId) {
        // Méthode simplifiée - à adapter selon votre implémentation réelle
        // Par défaut, retourne la position de fin de route
        return entryPoints.get(entryPoints.size() - 1);
    }

    public void updateTrafficLights() {
        // 1. Mettre à jour les niveaux de trafic pour chaque feu
        for (TrafficLight light : trafficLights) {
            int vehicleCount = countVehiclesApproaching(light);
            light.updateTrafficLevel(vehicleCount);
        }

        // 2. Prendre des décisions MDP pour chaque feu
        for (TrafficLight light : trafficLights) {
            light.mdpUpdate();
        }

        // 3. Appliquer la coordination entre feux
        coordinateTrafficLights();

        // 4. Mettre à jour l'état des feux
        for (TrafficLight light : trafficLights) {
            light.update();
        }
    }
    public void updateTrafficConditions() {
        this.isCongested = checkCongestion();
        // Mettre à jour les probabilités de transition basées sur le trafic global
        for (TrafficLight light : trafficLights) {
            int vehicleCount = countVehiclesApproaching(light);
            light.updateTrafficLevel(vehicleCount);
            light.updatePolicy(); // Recalculer la politique
        }
    }
}