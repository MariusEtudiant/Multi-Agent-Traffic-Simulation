package org.example.environment;

import org.example.agent.Position;
/*
Create an obstacle with a specific position
 */
public final class Obstacle {
    private Position position;
    public Obstacle(Position position) {
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }
}