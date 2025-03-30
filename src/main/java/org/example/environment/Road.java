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
    private static final int maxCapacity = 15;
    private final List<Position> entryPoints;  // entry points/end (intersections etc)
    private final List<TrafficLight> trafficLights;
    private final List<Lane> lanes;
    private boolean isCongested;


    // Construct
    public Road(String id, double length, int maxCapacity, List<Position> entryPoints) {
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
    private RoadCondition condition = RoadCondition.DRY;

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

}