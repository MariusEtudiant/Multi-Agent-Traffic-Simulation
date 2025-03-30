/*
Allows you to initialize a position along an x and y axis, which will later be used to manage the positions of vehicles, traffic lights, obstacles, etc.
 */

package org.example.agent;

public class Position {
    private final int x;
    private final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public double distanceTo(Position other) {
        int dx = x - other.x;
        int dy = y - other.y;
        return Math.sqrt(dx*dx + dy*dy);
    }

    // get
    public int getX() { return x; }
    public int getY() { return y; }

    //formats

    @Override
    public String toString() {
        return String.format("(%d, %d)", x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Position)) return false;
        Position other = (Position) obj;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

}