/*
Role: Represents the belief base of a BDI (Belief-Desire-Intention) agent, storing and managing the agent's
perceptions of its environment at a fairly low layer, i.e. lights, vehicle detections, obstacles, but no deeper coyances.

Main responsibilities
Maintains a set (HashSet<Belief>) of current beliefs.

Dynamic updating:

Synchronizes beliefs with the environment via updateBeliefs().

Query interface:

Allows you to check a belief (contains()) and display the complete state (toString()).
 */

package org.example.agent;

import org.example.environment.Lane;
import org.example.environment.Road;
import java.util.HashSet;
import java.util.Set;

public class BeliefInitial {
    private Set<Belief> beliefs;

    public BeliefInitial() {
        this.beliefs = new HashSet<>();
    }

    public void updateBeliefs(Lane lane,Road road, Vehicle vehicle) {
        beliefs.clear();

        // Feux de circulation
        boolean isGreen = lane.isTrafficLightGreen(road, road.getId());
        addBelief(new Belief("FeuVert", isGreen));
        addBelief(new Belief("FeuRouge", !isGreen));


        // Détection des véhicules
        addBelief(new Belief("CarAhead", lane.isCarAhead(vehicle)));
        addBelief(new Belief("CarOnLeft", lane.isCarOnLeft(vehicle)));
        addBelief(new Belief("CarOnRight", lane.isCarOnRight(vehicle)));

        // Obstacles
        addBelief(new Belief("ObstacleAhead", lane.isObstacleAhead(vehicle)));
        // État du trafic
        addBelief(new Belief("InTrafficJam", lane.isInTrafficJam()));

        // Véhicules prioritaires
        addBelief(new Belief("PriorityVehicle", lane.isPriorityVehicleNearby(vehicle)));


    }

    public void addBelief(Belief belief) {
        beliefs.add(belief);
    }
    public boolean contains(String name, Object value) {
        return beliefs.stream()
                .anyMatch(b -> b.getName().equals(name) && b.getValue().equals(value));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Belief belief : beliefs) {
            sb.append(belief.getName()).append("=").append(belief.getValue()).append(", ");
        }
        return !sb.isEmpty() ? sb.substring(0, sb.length() - 2) : "Aucune croyance";
    }
}
