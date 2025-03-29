package org.example.agent;

public class Desire {
    private String name;
    private boolean isAchieved;
    private int priority;

    public Desire(String name, int priority) {
        this.name = name;
        this.priority = priority;
        this.isAchieved = false;
    }

    public String getName() {
        return name;
    }
    public int getPriority() {
        return priority;
    }
    public boolean isAchieved() {
        return isAchieved;
    }
    public void achieve() {
        this.isAchieved = true;
    }
    public void setAchieved(boolean achieved) {
        isAchieved = achieved;
    }
    public void reset() {
        this.isAchieved = false;
    }
}
