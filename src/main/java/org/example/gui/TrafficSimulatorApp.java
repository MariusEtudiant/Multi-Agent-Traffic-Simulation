package org.example.gui;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.example.agent.Vehicle;
import org.example.environment.*;
import org.example.agent.Position;

import java.util.ArrayList;
import java.util.List;

import static org.example.environment.TrafficLight.LightColor.GREEN;

public class TrafficSimulatorApp extends Application {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private Canvas canvas;
    private GraphicsContext gc;
    private ComboBox<String> scenarioSelector;

    private Environment environment;
    private List<Vehicle> vehicles;
    private Road road;
    private Lane lane1;
    private TrafficLight trafficLight;
    private int step = 0;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Simulateur de Trafic Autonome");

        BorderPane root = new BorderPane();
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();

        root.setCenter(canvas);
        root.setTop(createControlPanel());

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();

        drawInitial();
    }

    private HBox createControlPanel() {
        HBox controlPanel = new HBox(10);

        Button startBtn = new Button("Démarrer la simulation");
        startBtn.setOnAction(e -> startSimulation());

        scenarioSelector = new ComboBox<>();
        scenarioSelector.getItems().addAll(
                "Scénario 1: Routes avec deux voies, un feu et des obstacles"
        );
        scenarioSelector.setValue("Scénario 1: Routes avec deux voies, un feu et des obstacles");

        controlPanel.getChildren().addAll(new Label("Choix du scénario:"), scenarioSelector, startBtn);
        return controlPanel;
    }

    private void drawInitial() {
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        drawScenario(scenarioSelector.getValue());
    }

    private void drawScenario(String scenario) {
        gc.clearRect(0, 0, WIDTH, HEIGHT);

        switch (scenario) {
            case "Scénario 1: Routes avec deux voies, un feu et des obstacles" -> drawScenario1();
        }
    }

    private void drawScenario1() {
        environment = new Environment();
        vehicles = new ArrayList<>();

        road = new Road("R1", 100.0, List.of(new Position(0, 0)));
        lane1 = new Lane("L1", 3.5, 1.0, Lane.DIRECTION_RIGHT, road);
        road.addLane(lane1);

        trafficLight = new TrafficLight("TL1", GREEN);
        road.addTrafficLight(trafficLight, new Position(80, 1));
        road.trainTrafficLights();
        road.enableMDP(true);
        road.setMDPDecisionInterval(3);

        Vehicle v1 = new Vehicle(new Position(0, 1), new Position(100, 1), environment);
        Vehicle v2 = new Vehicle(new Position(10, 1), new Position(100, 1), environment);
        lane1.addVehicle(v1);
        lane1.addVehicle(v2);
        vehicles.addAll(List.of(v1, v2));

        environment.getRoads().add(road);
        environment.buildGlobalGraph();

        drawFrame();
    }

    private void drawFrame() {
        gc.clearRect(0, 0, WIDTH, HEIGHT);

        // Route
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(100, 250, 600, 50);
        gc.setStroke(Color.WHITE);
        gc.setLineDashes(10);
        gc.strokeLine(100, 275, 700, 275);

        // Feu
        Color lightColor = switch (trafficLight.getState()) {
            case GREEN -> Color.GREEN;
            case ORANGE -> Color.ORANGE;
            case RED -> Color.RED;
        };
        gc.setFill(lightColor);
        gc.fillOval(680, 260, 20, 20);

        // Véhicules
        gc.setFill(Color.BLUE);
        for (Vehicle v : vehicles) {
            int x = (int) (v.getPosition().getX() * 7 + 100);
            int y = 270;
            gc.fillRect(x, y, 15, 10);
        }
    }

    private void startSimulation() {
        System.out.println("Simulation lancée pour : " + scenarioSelector.getValue());
        drawScenario(scenarioSelector.getValue());

        AnimationTimer timer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 500_000_000) {
                    step++;
                    System.out.printf("\n⏱️ Étape %02d\n", step);

                    road.updateTrafficConditions();
                    road.updateTrafficLights();

                    for (Vehicle v : new ArrayList<>(lane1.getVehicles())) {
                        v.bdiCycle(lane1, road);
                    }
                    lane1.removeArrivedVehicles();

                    drawFrame();
                    lastUpdate = now;
                }
            }
        };
        timer.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}