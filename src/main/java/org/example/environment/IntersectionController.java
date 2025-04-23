package org.example.environment;

// Coordination intelligente entre plusieurs TrafficLight
// Approche modulaire et extensible basée sur ton architecture

import org.example.environment.TrafficLight.LightColor;
import org.example.environment.TrafficLight.TrafficLevel;

import java.util.*;

public class IntersectionController {

    private final List<TrafficLight> trafficLights;
    private final Map<String, List<String>> conflictGroups; // Pour éviter les conflits de phases

    public IntersectionController(List<TrafficLight> lights) {
        this.trafficLights = lights;
        this.conflictGroups = defineConflictGroups();
    }

    // Exemple : feux de gauche-droite en conflit avec ceux de haut-bas
    private Map<String, List<String>> defineConflictGroups() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("group1", List.of("TL1", "TL3")); // N-S
        map.put("group2", List.of("TL2", "TL4")); // E-W
        return map;
    }

    public void stepAll() {
        Map<String, Double> groupScores = new HashMap<>();

        // 1. Score chaque groupe de feux
        for (Map.Entry<String, List<String>> group : conflictGroups.entrySet()) {
            double groupScore = 0.0;
            for (String id : group.getValue()) {
                TrafficLight tl = findById(id);
                if (tl != null) {
                    groupScore += scoreTrafficLight(tl);
                }
            }
            groupScores.put(group.getKey(), groupScore);
        }

        // 2. Choix du groupe prioritaire
        String bestGroup = groupScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (bestGroup == null) return;

        // 3. Appliquer actions : activer groupe prioritaire, bloquer les autres
        for (Map.Entry<String, List<String>> group : conflictGroups.entrySet()) {
            boolean activate = group.getKey().equals(bestGroup);
            for (String id : group.getValue()) {
                TrafficLight tl = findById(id);
                if (tl != null) {
                    if (activate) {
                        tl.setState(LightColor.GREEN);
                    } else {
                        tl.setState(LightColor.RED);
                    }
                }
            }
        }
    }

    private double scoreTrafficLight(TrafficLight tl) {
        TrafficLevel level = tl.getCurrentTraffic();
        return switch (level) {
            case NONE -> 0.0;
            case MEDIUM -> 3.0;
            case HEAVY -> 7.0;
        };
    }

    private TrafficLight findById(String id) {
        for (TrafficLight tl : trafficLights) {
            if (tl.getId().equals(id)) return tl;
        }
        return null;
    }

    public void printStatus() {
        System.out.println("\n--- État actuel de l'intersection ---");
        for (TrafficLight tl : trafficLights) {
            System.out.println("[" + tl.getId() + "] " + tl.getState() + " | Trafic: " + tl.getCurrentTraffic());
        }
    }
}
