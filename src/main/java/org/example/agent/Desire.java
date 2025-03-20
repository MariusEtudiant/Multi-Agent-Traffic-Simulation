package org.example.agent;

public class Desire {
    private String name;
    private boolean isAchieved;

    public Desire(String name) {
        this.name = name;
        this.isAchieved = false;
    }

    public String getName() {
        return name;
    }
    public boolean isAchieved() {
        return isAchieved;
    }
    public void setAchieved(boolean achieved) {
        isAchieved = achieved;
    }
}
