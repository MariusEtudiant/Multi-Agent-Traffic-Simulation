/*
package org.example.gui;

import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import org.example.agent.Vehicle;
import org.example.agent.Position;
import org.example.environment.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulationView extends AnimationTimer {
    private long lastUpdate = 0;
    private double simulationSpeed = 0.5; // 0.5 = moitié vitesse
    private final long BASE_INTERVAL = 16_666_667; // ~60 FPS
    double laneHeight = 40;
    double laneSpacing = 10; // Espace entre les lanes
    double baseY = 400; // Position Y de base

    private Road road;
    private List<Vehicle> vehicles;
    private Map<Vehicle, Circle> vehicleShapes = new HashMap<>();
    private List<Rectangle> laneShapes = new ArrayList<>();
    private Map<TrafficLight, Circle> lightShapes = new HashMap<>();
    private List<Circle> obstacleShapes = new ArrayList<>();
    private Pane root;  // Conteneur pour les éléments graphiques

    public SimulationView(Pane rootPane) {
        this.root = rootPane;
        root.setPrefSize(1200, 800);
        initializeSimulation();
        drawInitialScene();
        start();
    }

    @Override
    public void handle(long now) {
        if (now - lastUpdate >= BASE_INTERVAL / simulationSpeed) {
            updateSimulation();
            lastUpdate = now;
        }
    }

    public void setSimulationSpeed(double speed) {
        this.simulationSpeed = speed;
    }

    private void initializeSimulation() {
        List<Position> entries = List.of(new Position(0, 0), new Position(100, 0));
        this.road = new Road("R1", 100.0, entries);
        this.road.initGraphForPathfinding();
        Lane lane = new Lane("L1", 3.5, 0, Lane.DIRECTION_RIGHT, road);
        Lane lane1 = new Lane("L2", 3.5, 0, Lane.DIRECTION_RIGHT, road);
        road.addLane(lane);
        road.addLane(lane1);

        TrafficLight light = new TrafficLight("R1", TrafficLight.LightColor.GREEN);
        road.addTrafficLight(light, new Position(100, 0));

        Obstacle obs = new Obstacle(new Position(50, 0));
        lane.addObstacle(obs);

        this.vehicles = new ArrayList<>();
       Vehicle v1 = new Vehicle(new Position(0, 0), new Position(100, 0));
        Vehicle v2 = new Vehicle(new Position(10, 1), new Position(100, 0));
        vehicles.add(v1);
        vehicles.add(v2);
        lane.addVehicle(v1);
        lane1.addVehicle(v2);
    }

    private void drawInitialScene() {
        root.getChildren().clear();

        // Draw lanes as rectangles
        for (int i = 0; i < road.getLanes().size(); i++) {
            Lane lane = road.getLanes().get(i);
            // Décalage vertical en fonction de l'index de la lane
            double laneY = baseY - (i * (laneHeight + laneSpacing));

            Rectangle laneRect = new Rectangle(0, laneY, 1000, laneHeight);
            laneRect.setFill(Color.LIGHTGRAY);
            laneRect.setStroke(Color.DARKGRAY); // Bordure pour mieux voir les lanes
            root.getChildren().add(laneRect);
            laneShapes.add(laneRect);
        }

        // Draw traffic lights
        for (TrafficLight light : road.getTrafficLights()) {
            Position pos = road.getLightPosition(light.getId());
            Circle circle = new Circle(pos.getX() * 10, 400, 10);
            circle.setFill(convertColor(light.getState()));
            lightShapes.put(light, circle);
            root.getChildren().add(circle);
        }

        // Draw obstacles
        for (Lane lane : road.getLanes()) {
            for (Obstacle obstacle : lane.getObstacles()) {
                Position pos = obstacle.getPosition();
                Circle obs = new Circle(pos.getX() * 10, 400, 6);
                obs.setFill(Color.DARKRED);
                obstacleShapes.add(obs);
                root.getChildren().add(obs);
            }
        }

        // Draw vehicles
        for (Vehicle v : vehicles) {
            Position p = v.getPosition();
            Circle circle = new Circle(p.getX() * 10, 400, 8);
            circle.setFill(Color.BLUE);
            vehicleShapes.put(v, circle);
            root.getChildren().add(circle);
        }
    }

    public void updateSimulation() {
        road.updateTrafficConditions();
        road.updateTrafficLights();

        // Update traffic lights' colors
        for (Map.Entry<TrafficLight, Circle> entry : lightShapes.entrySet()) {
            TrafficLight light = entry.getKey();
            Circle shape = entry.getValue();
            shape.setFill(convertColor(light.getState()));
        }

        // Update vehicles' positions
        for (Vehicle v : vehicles) {
            v.bdiCycle(road.getLanes().get(0), road);
            Position pos = v.getPosition();
            Circle circle = vehicleShapes.get(v);
            if (circle != null) {
                circle.setCenterX(pos.getX() * 10);
            }
        }
    }

    private Color convertColor(TrafficLight.LightColor c) {
        return switch (c) {
            case GREEN -> Color.LIMEGREEN;
            case RED -> Color.RED;
            case ORANGE -> Color.ORANGE;
        };
    }
}
 */