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
import org.example.environment.TrafficLight;

import java.util.HashSet;
import java.util.Set;

import static org.example.environment.TrafficLight.LightColor.*;

public class BeliefInitial {
    private Set<Belief> beliefs;

    public BeliefInitial() {
        this.beliefs = new HashSet<>();
    }

    public void updateBeliefs(Lane lane,Road road, Vehicle vehicle) {
        beliefs.clear();

        // Feux de circulation
        TrafficLight.LightColor color = lane.checkState(road, road.getId());
        if (color == RED) {  // Utilisez == pour les enum
            addBelief(new Belief("FeuRED", true));
            addBelief(new Belief("FeuOrange", false));
            addBelief(new Belief("FeuVert", false));
        } else if (color == GREEN) {
            addBelief(new Belief("FeuVert", true));
            addBelief(new Belief("FeuOrange", false));
            addBelief(new Belief("FeuRED", false));
        } else if (color == ORANGE) {  // Ajout explicite pour orange
            addBelief(new Belief("FeuOrange", true));
            addBelief(new Belief("FeuRED", false));
            addBelief(new Belief("FeuVert", false));
        } else {
            System.out.println("Etat inconnu: " + color);
        }


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
