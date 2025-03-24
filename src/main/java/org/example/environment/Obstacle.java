package org.example.environment;

import org.example.agent.Position;

public final class Obstacle {
    // Attribut pour stocker la position de l'obstacle
    private Position position;

    // Constructeur pour initialiser l'obstacle avec une position
    public Obstacle(Position position) {
        this.position = position;
    }

    // Méthode pour obtenir la position de l'obstacle
    public Position getPosition() {
        return position;
    }

    // Optionnel : Méthode pour mettre à jour la position de l'obstacle
    public void setPosition(Position position) {
        this.position = position;
    }
}