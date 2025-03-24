package org.example.environment;

import org.example.agent.Vehicle;

import java.util.ArrayList;
import java.util.List;


public class Environment {
    private int[][] grid;
    private List<Road> roads;
    //private List<In> intersections;
    public Environment(){
        this.roads = new ArrayList<>();
    }

}
