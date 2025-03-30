package org.example.environment;

import org.example.agent.Vehicle;

import java.util.ArrayList;
import java.util.List;

/*
the basic environment, which will then include roads, which in turn will include vehicles, etc. etc.

NOTE: we'll use grid later to create a 2d interface for the moment we're content with the text output,
giving priority to the functional over the superficial.
 */

public class Environment {
    private int[][] grid;
    private List<Road> roads;
    //private List<In> intersections;
    public Environment(){
        this.roads = new ArrayList<>();
    }

}
