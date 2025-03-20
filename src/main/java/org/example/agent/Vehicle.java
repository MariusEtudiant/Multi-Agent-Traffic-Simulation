package org.example.agent;
import org.example.environment.Environment;

import java.util.ArrayList;
import java.util.List;

public class Vehicle {
    private BeliefInitial beliefs;
    private List<Desire> desires;
    private List<Intention> intentions;
    private Position position;
    private static final double SAFE_DISTANCE = 10.0;

    public Vehicle(Position position) {
        this.beliefs = new BeliefInitial();
        this.desires = new ArrayList<>();
        this.intentions = new ArrayList<>();
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }
    public void setPosition(Position position) {
        this.position = position;
    }
    public List<Desire> getDesires() {
        return desires;
    }
    public void perceivedEnvironment(Environment env){
        beliefs.updateBeliefs(env, this);
    }
    public void addDesire(Desire desire) {
        desires.add(desire);
    }
    public BeliefInitial getBeliefs() {
        return beliefs;
    }
    public void addIntention(Intention intention) {
        intentions.add(intention);
    }
    public boolean isCollisionImminent(Environment env){
        for(Vehicle other : env.getVehicles()) {
            if (!this.equals(other) && isTooClose(other)) {
                return true;
            }
        }
        return false;
    }
    public List<Intention> getIntentions() {
        return intentions;
    }
    public boolean isTooClose(Vehicle other){
        return this.position.distanceTo(other.getPosition()) < SAFE_DISTANCE;
    }
    public void decideNextAction(Environment env) {
        intentions.clear(); // Réinitialiser les intentions

        if (isCollisionImminent(env)) {
            addIntention(Intention.SLOW_DOWN);
        } else if (beliefs.contains("FeuVert", true)) {
            addIntention(Intention.ACCELERATE);
        } else if (beliefs.contains("FeuRouge", true)) {
            addIntention(Intention.STOP);
        } else if (beliefs.contains("CarAhead", true)) {
            addIntention(Intention.TURN_LEFT); // Essayer de changer de voie
        } else {
            addIntention(Intention.ACCELERATE); // Continuer tout droit
        }
    }
    public void act() {
        for (Intention intention : intentions) {
            switch (intention) {
                case ACCELERATE:
                    position = new Position(position.getX() + 1, position.getY()); // Avancer en X
                    break;
                case SLOW_DOWN:
                    position = new Position(position.getX() - 1, position.getY()); // Reculer en X
                    break;
                case TURN_LEFT:
                    position = new Position(position.getX(), position.getY() - 1); // Tourner à gauche
                    break;
                // Ajoutez d'autres cas si nécessaire
            }
        }
    }
}
