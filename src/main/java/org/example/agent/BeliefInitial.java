package org.example.agent;

import com.sun.source.tree.UsesTree;
import org.example.environment.Environment;
import org.example.agent.Vehicle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    public void updateBeliefs(Environment env, Vehicle vehicle) {
        beliefs.clear();
        // Exemple : Mettre à jour les croyances en fonction des feux de circulation
        if (env.isTrafficLightGreen("IntersectionA")) {
            addBelief(new Belief("FeuVert", true));
        } else {
            addBelief(new Belief("FeuVert", false));
        }

        // Exemple : Mettre à jour les croyances en fonction des autres véhicules
        if (env.isCarAhead(vehicle)) {
            addBelief(new Belief("CarAhead", true));
        } else {
            addBelief(new Belief("CarAhead", false));
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
