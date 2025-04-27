package org.example.environment;

import org.example.agent.Vehicle;
import org.example.agent.Position;
import org.example.planning.Graph;
import org.example.planning.GraphNode;

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
    private final List<Position> entryPoints;  // entry points/end
    private final List<TrafficLight> trafficLights;
    private List<Position> trafficLightPositions = new ArrayList<>();
    private final List<Lane> lanes;
    private boolean isCongested;
    private RoadCondition condition = RoadCondition.DRY;
    private Graph graph;

    //MDP control
    private boolean useMDP = true;
    private int mdpDecisionInterval = 5;
    private int tickCounter = 0;

    //Construct
    public Road(String id, double length, List<Position> entryPoints) {
        this.id = id;
        this.length = length;
        this.entryPoints = entryPoints;
        this.isCongested = false;
        this.trafficLights = new ArrayList<>();
        this.lanes = new ArrayList<>();
    }
    public enum RoadCondition{
        DRY(1.0), WET(0.7), ICY(0.3);
        private final double frictionFactor;
        RoadCondition(double frictionFactor) {
            this.frictionFactor = frictionFactor;
        }
    }

    public void addLane(Lane lane) {
        lanes.add(lane);
        lane.setRoad(this);
    }
    public static int maxCapacityCount(){return maxCapacity;}
    public void addTrafficLight(TrafficLight trafficLight, Position position){
        trafficLights.add(trafficLight);
        trafficLightPositions.add(position);
    }

    public boolean hasLeftLane(Lane currentLane) {
        int index = lanes.indexOf(currentLane);
        return index > 0;
    }
    public boolean hasRightLane(Lane currentLane) {
        int index = lanes.indexOf(currentLane);
        return index < lanes.size() - 1;
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
        //trouver le feu avec le plus de trafic
        TrafficLight busiestLight = null;
        int maxTraffic = -1;

        for (TrafficLight light : trafficLights) {
            int trafficValue = light.getCurrentTraffic().ordinal();
            if (trafficValue > maxTraffic) {
                maxTraffic = trafficValue;
                busiestLight = light;
            }
        }
        //mettre le feu le plus chargé au vert, les autres au rouge
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

    public void enableMDP(boolean enable) {
        this.useMDP = enable;
    }

    public void setMDPDecisionInterval(int interval) {
        this.mdpDecisionInterval = interval;
    }

    public void trainTrafficLights() {
        for (TrafficLight light : trafficLights) {
            light.printTransitionMatrix();
            light.valueIteration(0.005);
            light.printPolicy();
        }
    }

    public int countVehiclesApproaching(TrafficLight light) {
        final double APPROACH_DISTANCE = 30.0; // constante pour la distance
        Position lightPosition = getTrafficLightPosition(light);

        return (int) lanes.stream()
                .flatMap(lane -> lane.getVehicles().stream())
                .filter(v -> v.getPosition().distanceTo(lightPosition) < APPROACH_DISTANCE)
                .count();
    }

    public void updateTrafficLights() {
        tickCounter++;

        //1)Mettre à jour les niveaux de trafic pour chaque feu
        for (TrafficLight light : trafficLights) {
            int vehicleCount = countVehiclesApproaching(light);
            light.updateTrafficLevel(vehicleCount);
        }

        //2)Appliquer les décisions MDP seulement tous les X ticks
        if (useMDP && tickCounter % mdpDecisionInterval == 0) {
            for (TrafficLight light : trafficLights) {
                light.mdpUpdate();
            }
        }

        //3)Coordination possible entre les feux (si plusieurs)
        coordinateTrafficLights();

        //4)Mise à jour d’état (permet les cycles ORANGE -> ROUGE, etc.)
        for (TrafficLight light : trafficLights) {
            light.update();
        }
    }

    public void updateTrafficConditions() {
        this.isCongested = checkCongestion();
        //Mettre à jour les probabilités de transition basées sur le trafic global
        for (TrafficLight light : trafficLights) {
            int vehicleCount = countVehiclesApproaching(light);
            light.updateTrafficLevel(vehicleCount);
            light.updatePolicy(); //recalcule de la politique
        }
    }

    public void initGraphForPathfinding() {
        this.graph = new Graph();
        int segmentLength = 10;

        List<Position> obstaclePositions = new ArrayList<>();
        for (Lane lane : lanes) {
            for (Obstacle obstacle : lane.getObstacles()) {
                obstaclePositions.add(obstacle.getPosition());
            }
        }

        //créer tous les noeuds possibles
        for (int x = 0; x <= this.length; x += segmentLength) {
            for (Lane lane : lanes) {
                Position pos = new Position(x, lane.getCenterYInt());
                if (!obstaclePositions.contains(pos)) {
                    graph.getOrCreateNode(pos);
                }
            }
        }
        //connecter horizontalement
        for (int x = 0; x <= this.length - segmentLength; x += segmentLength) {
            for (Lane lane : lanes) {
                Position from = new Position(x, lane.getCenterYInt());
                Position to = new Position(x + segmentLength, lane.getCenterYInt());

                if (graph.getNode(from) != null && graph.getNode(to) != null) {
                    boolean obstacleBetween = false;
                    for (Position obstaclePos : obstaclePositions) {
                        double obsX = obstaclePos.getX();
                        int obsY = obstaclePos.getY();
                        if (obsY == from.getY() && obsX >= from.getX() - 1 && obsX <= to.getX() + 1) {
                            obstacleBetween = true;
                            break;
                        }
                    }
                    if (!obstacleBetween) {
                        graph.connect(from, to, segmentLength);
                    }
                }
            }
        }


        //connecter verticalement (changement de voie)
        for (int x = 0; x <= this.length; x += segmentLength) {
            for (int i = 0; i < lanes.size() - 1; i++) {
                int y1 = lanes.get(i).getCenterYInt();
                int y2 = lanes.get(i + 1).getCenterYInt();

                Position fromTop = new Position(x, y1);
                Position toBottom = new Position(x, y2);

                if (graph.getNode(fromTop) != null && graph.getNode(toBottom) != null) {
                    graph.connect(fromTop, toBottom, 5.0); // changement de voie
                    graph.connect(toBottom, fromTop, 5.0);
                }
            }
        }

        System.out.println("Graphe généré automatiquement sans obstacles, " + graph.getAllNodes().size() + " noeuds.");
    }
    public Graph getGraph() {
        return graph;
    }
    public List<Position> getEntryPoints() {
        return entryPoints;
    }
    public List<Lane> getLanes() {
        return new ArrayList<>(lanes);
    }
    public Position getTrafficLightPosition(TrafficLight light) {
        int index = trafficLights.indexOf(light);
        if (index != -1 && index < trafficLightPositions.size()) {
            return trafficLightPositions.get(index);
        }
        return null;
    }

    public Lane getAdjacentLane(Lane currentLane, boolean toLeft) {
        List<Lane> allLanes = this.getLanes();
        allLanes.sort((l1, l2) -> Double.compare(l2.getCenterY(), l1.getCenterY()));
        int index = allLanes.indexOf(currentLane);
        if (index == -1) return null;
        int targetIndex = toLeft ? index - 1 : index + 1;
        if (targetIndex < 0 || targetIndex >= allLanes.size()) return null;
        Lane targetLane = allLanes.get(targetIndex);
        if (targetLane.getDirection() == currentLane.getDirection()) {
            return targetLane;
        }

        return null;
    }
    public String getName() {
        return id;
    }
    public void setCondition(RoadCondition condition) {
        this.condition = condition;
    }

    public RoadCondition getCondition() {
        return condition;
    }
}