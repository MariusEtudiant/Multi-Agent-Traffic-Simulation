package org.example.agent;
import org.example.environment.Environment;
import org.example.environment.Lane;
import org.example.environment.Road;
import org.example.logic.AndFormula;
import org.example.logic.AtomFormula;
import org.example.logic.LogicalFormula;
import org.example.logic.NotFormula;

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
    public void perceivedEnvironment(Lane lane, Road road){
        beliefs.updateBeliefs(lane,road,this);
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

    public List<Intention> getIntentions() {
        return intentions;
    }

    public void decideNextAction(Lane lane) {
        intentions.clear(); // Réinitialiser les intentions

        LogicalFormula feuVert = new AtomFormula("FeuVert", true);
        LogicalFormula carAhead = new AtomFormula("CarAhead", true);
        LogicalFormula carOnLeft = new AtomFormula("CarOnLeft", true);
        LogicalFormula carOnRight = new AtomFormula("CarOnRight", true);
        LogicalFormula isObstacle = new AtomFormula("IsObstacle", true);
        LogicalFormula canAccelerate = new AndFormula(feuVert,new NotFormula(carAhead)); // feuvert et non voiture devant
        // Évaluer les conditions
        if (canAccelerate.evaluate(beliefs)) {
            System.out.println("Condition : canAccelerate -> ACCELERATE");
            addIntention(Intention.ACCELERATE);
        } else if (carAhead.evaluate(beliefs)) {
            System.out.println("Condition : carAhead -> SLOW_DOWN");
            addIntention(Intention.SLOW_DOWN);
        } else if (feuVert.evaluate(beliefs)) {
            System.out.println("Condition : feuVert -> ACCELERATE");
            addIntention(Intention.ACCELERATE);
        } else {
            System.out.println("Condition : default -> STOP");
            addIntention(Intention.STOP);
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
                case TURN_RIGHT:
                    position = new Position(position.getX(), position.getY() + 1); // Tourner à droite
                    break;
                // Ajoutez d'autres cas si nécessaire
            }
        }
    }
}
