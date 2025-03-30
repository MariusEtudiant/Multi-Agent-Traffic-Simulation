package org.example.environment;
/*
a trafficLight with an id, state, interval
 */
public class TrafficLight {
    private String id;
    private  String state;
    private int changeInterval = 10;
    private int stepCount = 0;


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

    public void update(){
        stepCount++;
        if (stepCount % changeInterval == 0) {
            toggleState();
            stepCount = 0;
        }
    }

    public void toggleState() {
        if ("GREEN".equals(state)) {
            state = "RED";
        } else {
            state = "GREEN";
        }
    }
}
