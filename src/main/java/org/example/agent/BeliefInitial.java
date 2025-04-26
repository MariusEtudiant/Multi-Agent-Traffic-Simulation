package org.example.agent;

import org.example.environment.Lane;
import org.example.environment.Road;
import org.example.environment.TrafficLight;

import java.util.HashSet;
import java.util.Set;

import static org.example.environment.TrafficLight.LightColor.*;

/**
 * 📚 BeliefInitial
 *
 * Représente la base de croyances d’un agent BDI (Belief-Desire-Intention).
 * Cette classe gère toutes les perceptions bas-niveau de l’environnement pour un véhicule autonome :
 * - État des feux de circulation
 * - Véhicules et obstacles proches
 * - État du trafic
 *
 * 🔁 Rôle principal :
 * - Synchroniser les croyances avec l’environnement via updateBeliefs()
 * - Fournir un accès rapide aux croyances via contains() ou toString()
 */
public class BeliefInitial {

    // Ensemble des croyances actuelles du véhicule
    private Set<Belief> beliefs;

    /** Constructeur : initialise un ensemble vide de croyances. */
    public BeliefInitial() {
        this.beliefs = new HashSet<>();
    }

    /**
     * 🔄 Met à jour dynamiquement les croyances de l’agent à partir de la perception de son environnement immédiat.
     *
     * @param lane     La voie actuelle du véhicule
     * @param road     La route actuelle
     * @param vehicle  Le véhicule lui-même
     */
    public void updateBeliefs(Lane lane, Road road, Vehicle vehicle) {
        beliefs.clear();

        // Récupération de la position du premier feu (⚠️ à améliorer pour plusieurs feux)
        Position lightPos = road.getTrafficLightPosition(road.getTrafficLights().get(0));

        boolean feuDevant = lightPos != null && lightPos.getX() > vehicle.getPosition().getX();
        double distanceToFeu = (lightPos != null) ? lightPos.getX() - vehicle.getPosition().getX() : Double.MAX_VALUE;
        boolean feuFranchi = lightPos != null && vehicle.getPosition().getX() > lightPos.getX();

        // 🧠 Croyance : feu franchi (permet d’ignorer les anciens feux dans les raisonnements)
        addBelief(new Belief("FeuFranchi", feuFranchi));
        addBelief(new Belief("FeuDevant", feuDevant));

        // 🟡 Zone d’influence du feu : si dans les 15m → on commence à réagir
        boolean dansZoneInfluence = feuDevant && distanceToFeu <= 15;

        // 🔦 Croyances sur la couleur du feu (uniquement si on est dans la zone d'influence)
        if (dansZoneInfluence) {
            TrafficLight.LightColor color = lane.checkState(road, road.getId());
            addBelief(new Belief("FeuRouge", color == RED));
            addBelief(new Belief("FeuOrange", color == ORANGE));
            addBelief(new Belief("FeuVert", color == GREEN));
        } else {
            // Hors zone : ignorer l’état du feu
            addBelief(new Belief("FeuRouge", false));
            addBelief(new Belief("FeuOrange", false));
            addBelief(new Belief("FeuVert", false));
        }

        // 🚗 Véhicules détectés dans l’environnement immédiat
        addBelief(new Belief("CarAhead", lane.isCarAhead(vehicle)));
        addBelief(new Belief("CarOnLeft", lane.isCarOnLeft(vehicle)));
        addBelief(new Belief("CarOnRight", lane.isCarOnRight(vehicle)));

        // 🧱 Obstacles sur la voie
        addBelief(new Belief("ObstacleAhead", lane.isObstacleAhead(vehicle)));

        // 🚧 Embouteillage détecté
        addBelief(new Belief("InTrafficJam", lane.isInTrafficJam()));

        // 🚨 Véhicule prioritaire proche
        addBelief(new Belief("PriorityVehicle", lane.isPriorityVehicleNearby(vehicle)));
    }

    /**
     * ➕ Ajoute une nouvelle croyance à l’ensemble.
     */
    public void addBelief(Belief belief) {
        beliefs.add(belief);
    }

    /**
     * 🔍 Vérifie la présence d’une croyance (nom + valeur).
     *
     * @param name  Nom de la croyance (ex: "FeuRouge")
     * @param value Valeur attendue (ex: true)
     * @return true si elle est présente
     */
    public boolean contains(String name, Object value) {
        return beliefs.stream()
                .anyMatch(b -> b.getName().equals(name) && b.getValue().equals(value));
    }

    /**
     * 🖨️ Affiche l’ensemble des croyances actuelles sous forme de texte.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Belief belief : beliefs) {
            sb.append(belief.getName()).append("=").append(belief.getValue()).append(", ");
        }
        return !sb.isEmpty() ? sb.substring(0, sb.length() - 2) : "Aucune croyance";
    }
}
