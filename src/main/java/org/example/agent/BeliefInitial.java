package org.example.agent;

import org.example.environment.Road;
import java.util.HashSet;
import java.util.Set;

public class BeliefInitial {
    private Set<Belief> beliefs;

    public BeliefInitial() {
        this.beliefs = new HashSet<>();
    }
    public void addBelief(Belief belief) {
        beliefs.add(belief);
    }
    public boolean contains(String name, Object value) {
        return beliefs.stream()
                .anyMatch(b -> b.getName().equals(name) && b.getValue().equals(value));
    }
    public void updateBeliefs(Road road, Vehicle vehicle) {
        beliefs.clear();
        // Exemple : Mettre à jour les croyances en fonction des feux de circulation
        if (road.isTrafficLightGreen("Road1")) {
            addBelief(new Belief("FeuVert", true));
        } else {
            addBelief(new Belief("FeuVert", false));
        }

        // Exemple : Mettre à jour les croyances en fonction des autres véhicules
        if (road.isCarAhead(vehicle)) {
            addBelief(new Belief("CarAhead", true));
        } else {
            addBelief(new Belief("CarAhead", false));
        }

        if (road.isCarBehind(vehicle)){
            addBelief(new Belief("CarBehind", true));
        }else {
            addBelief(new Belief("CarBehind", false));
        }
        if(road.isObstacleAhead(vehicle)){
            addBelief(new Belief("ObstacleAhead", true));
        }else{
            addBelief(new Belief("ObstacleAhead", false));
        }
        System.out.println("Croyances mises à jour : " + beliefs);
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
