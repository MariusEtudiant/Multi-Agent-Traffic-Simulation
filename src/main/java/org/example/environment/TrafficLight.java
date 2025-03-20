package org.example.environment;

public class TrafficLight {
    private String id;
    private  String state;

    public TrafficLight(String id, String state) {
        this.id = id;
        this.state = state;
    }
    public String getState(){
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }
    public String getId() {
        return id;
    }
    public void toggleState() {
        if ("GREEN".equals(state)) {
            state = "RED";
        } else {
            state = "GREEN";
        }
    }
}
