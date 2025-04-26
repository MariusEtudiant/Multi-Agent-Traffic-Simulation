package org.example.gui;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.example.agent.Intention;
import org.example.agent.Position;
import org.example.agent.Vehicle;
import org.example.environment.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.example.agent.Intention.*;
import static org.example.environment.TrafficLight.LightColor.GREEN;

public class TrafficSimulatorApp extends Application {

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;

    private Canvas canvas;
    private GraphicsContext gc;
    private ComboBox<String> scenarioSelector;

    private Environment environment;
    private List<Vehicle> vehicles;
    private Road road;
    private Lane lane1;
    private Lane lane2;
    private TrafficLight trafficLight;
    private int step = 0;
    private final List<TrafficLight.LightColor> feuStates = new ArrayList<>();

    private AnimationTimer simulationTimer;
    private boolean showPaths = false; // ✅ Global
    private Vehicle selectedVehicle = null;
    private XYChart.Series<Number, Number> lane1Series;
    private XYChart.Series<Number, Number> lane2Series;
    private int trafficStep = 0;



    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Simulateur de Trafic Autonome");

        BorderPane root = new BorderPane();
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();

        root.setCenter(canvas);
        root.setTop(createControlPanel(root));
        canvas.setOnMouseClicked(event -> {
            if (selectedVehicle != null) {
                System.out.println("🛣️ Chemin complet de V" + selectedVehicle.getId() + " :");
                for (Position pos : selectedVehicle.getPath()) {
                    System.out.println("   ➔ " + pos);
                }
            }

            double clickedX = (event.getX() - 100) / 7.0;

            // Cherche un véhicule proche du clic
            for (Vehicle v : vehicles) {
                double vx = v.getPreciseX();
                if (Math.abs(vx - clickedX) < 2.0) {
                    selectedVehicle = v;
                    showVehicleInfoPopup(v);
                    break;
                }
            }
        });
        LineChart<Number, Number> trafficChart = createTrafficChart();
        root.setRight(trafficChart);

        Scene scene = new Scene(root, WIDTH+ 500, HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();

        drawInitial();
    }

    private HBox createControlPanel(BorderPane root) {
        HBox controlPanel = new HBox(10);

        Button startBtn = new Button("Démarrer la simulation");
        startBtn.setOnAction(e -> startSimulation());

        Button showChartBtn = new Button("Afficher évolution feu");
        showChartBtn.setOnAction(e -> showTrafficLightChart());

        Button mdpToggleBtn = new Button("Désactiver MDP");
        mdpToggleBtn.setOnAction(e -> {
            boolean newState = !road.getTrafficLights().getFirst().isUseMDP();
            for (TrafficLight light : road.getTrafficLights()) {
                light.setUseMDP(newState);
            }
            mdpToggleBtn.setText(newState ? "Désactiver MDP" : "Activer MDP");
            System.out.println("🧠 MDP " + (newState ? "activé" : "désactivé") + " !");
        });

        Button showRewardChartBtn = new Button("Récompenses MDP");
        showRewardChartBtn.setOnAction(e -> showCumulativeRewardChart());

        Button togglePathBtn = new Button("Afficher chemins");
        togglePathBtn.setOnAction(e -> {
            showPaths = !showPaths;
            togglePathBtn.setText(showPaths ? "Masquer chemins" : "Afficher chemins");
            drawFrame(); // Forcer redessin
        });

        scenarioSelector = new ComboBox<>();
        scenarioSelector.getItems().addAll(
                "Scénario 1: Routes avec deux voies, un feu et des obstacles"
        );
        scenarioSelector.setValue("Scénario 1: Routes avec deux voies, un feu et des obstacles");

        controlPanel.getChildren().addAll(new Label("Choix du scénario:"), scenarioSelector, startBtn, showChartBtn, mdpToggleBtn, showRewardChartBtn, togglePathBtn);
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

        road = new Road("R1", 120.0, List.of(new Position(0, 0)));

        lane1 = new Lane("L1", 3.5, 1.0, Lane.DIRECTION_RIGHT, road);
        lane2 = new Lane("L2", 3.5, -1.0, Lane.DIRECTION_RIGHT, road);
        road.addLane(lane1);
        road.addLane(lane2);

        trafficLight = new TrafficLight("R1", GREEN);
        road.addTrafficLight(trafficLight, new Position(70, 1));
        road.trainTrafficLights();
        road.enableMDP(true);
        road.setMDPDecisionInterval(5);

        Vehicle v1 = new Vehicle(new Position(15, 1), new Position(100, -1), environment);
        //Vehicle v2 = new Vehicle(new Position(35, 1), new Position(100, -1), environment);
        lane1.addVehicle(v1);
        //lane1.addVehicle(v2);

        //Vehicle v3 = new Vehicle(new Position(0, -1), new Position(100, 1), environment);
        //Vehicle v4 = new Vehicle(new Position(11, -1), new Position(100, 1), environment);
        //lane2.addVehicle(v3);
        //lane2.addVehicle(v4);

        vehicles.addAll(List.of(v1));//, v2, v3, v4));
        v1.setPathColor(Color.BLUE);
        //v2.setPathColor(Color.GREEN);
        //v3.setPathColor(Color.RED);
        //v4.setPathColor(Color.PURPLE);
        // Ajouter les obstacles
        Obstacle obs1 = new Obstacle(new Position(50, 1));     // Sur la lane du haut
        Obstacle obs2 = new Obstacle(new Position(20, -1));    // Sur la lane du bas
        Obstacle obs4 = new Obstacle(new Position(35, -1));

        lane1.addObstacle(obs1);
        lane2.addObstacle(obs2);
        lane2.addObstacle(obs4);


        environment.getRoads().add(road);
        environment.buildGlobalGraph();

        for (Obstacle obs : lane1.getObstacles()) {
            environment.getGlobalGraph().markObstacle(obs.getPosition());
        }
        for (Obstacle obs : lane2.getObstacles()) {
            environment.getGlobalGraph().markObstacle(obs.getPosition());
        }

        drawFrame();
    }


    private void drawFrame() {
        gc.clearRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.DARKGRAY);
        gc.fillRect(100, 230, 700, 20);
        gc.fillRect(100, 250, 700, 20);

        gc.setStroke(Color.WHITE);
        gc.setLineDashes(10);
        gc.strokeLine(100, 250, 800, 250);

        Color lightColor = switch (trafficLight.getState()) {
            case GREEN -> Color.GREEN;
            case ORANGE -> Color.ORANGE;
            case RED -> Color.RED;
        };
        gc.setFill(lightColor);
        gc.fillOval(70 * 7 + 100, 232, 20, 20); // Feu à x=70 logique

        gc.setFill(Color.BLUE);
        for (Vehicle v : vehicles) {
            int x = (int) (v.getPreciseX() * 7 + 100);
            int y = (v.getPosition().getY() == 1) ? 232 : 252;
            gc.fillRect(x, y, 15, 10);
        }

        // Obstacles
        gc.setFill(Color.RED);
        for (Obstacle o : lane1.getObstacles()) {
            int x = (int) (o.getPosition().getX() * 7 + 100);
            gc.fillRect(x, 232, 10, 10);
        }
        for (Obstacle o : lane2.getObstacles()) {
            int x = (int) (o.getPosition().getX() * 7 + 100);
            gc.fillRect(x, 252, 10, 10);
        }

        // 🧭 Chemins si activé
        if (showPaths) {
            for (Vehicle v : vehicles) {
                gc.setStroke(v.getPathColor());
            }
            gc.setLineDashes(0);
            for (Vehicle v : vehicles) {
                List<Position> path = v.getPath();
                if (path == null || path.isEmpty()) continue;

                Position prev = null;
                for (Position pos : path) {
                    if (prev != null) {
                        int x1 = (int) (prev.getX() * 7 + 100);
                        int y1 = (prev.getY() == 1) ? 237 : (prev.getY() == -1 ? 257 : 247);
                        int x2 = (int) (pos.getX() * 7 + 100);
                        int y2 = (pos.getY() == 1) ? 237 : (pos.getY() == -1 ? 257 : 247);
                        gc.strokeLine(x1, y1, x2, y2);
                    }
                    prev = pos;
                }

                // 🔵 Direction vers prochain waypoint
                if (v.getNextWaypointIndex() < path.size()) {
                    Position next = path.get(v.getNextWaypointIndex());
                    int fromX = (int) (v.getPreciseX() * 7 + 100);
                    int fromY = (v.getPosition().getY() == 1) ? 237 : 257;
                    int toX   = (int) (next.getX() * 7 + 100);
                    int toY   = (next.getY() == 1) ? 237 : 257;

                    gc.setStroke(Color.LIMEGREEN);
                    gc.strokeLine(fromX, fromY, toX, toY);
                }

                // 🔘 Points sur les waypoints suivants
                gc.setFill(Color.GRAY);
                for (int i = v.getNextWaypointIndex(); i < path.size(); i++) {
                    Position wp = path.get(i);
                    int wx = (int) (wp.getX() * 7 + 100);
                    int wy = (wp.getY() == 1) ? 237 : 257;
                    gc.fillOval(wx - 2, wy - 2, 4, 4);
                }
            }
        }
    }

    private void startSimulation() {
        if (simulationTimer != null) {
            System.out.println("⏳ Simulation déjà en cours !");
            return;
        }
        System.out.println("Simulation lancée pour : " + scenarioSelector.getValue());
        drawScenario(scenarioSelector.getValue());

        simulationTimer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 300_000_000) {
                    step++;
                    System.out.printf("\n⏱️ Étape %02d\n", step);

                    road.updateTrafficConditions();  // ⬅️ Juste ici !

                    // ✅ Ajouter les données du graphique
                    int count1 = lane1.getVehicles().size();
                    int count2 = lane2.getVehicles().size();
                    lane1Series.getData().add(new XYChart.Data<>(trafficStep, count1));
                    lane2Series.getData().add(new XYChart.Data<>(trafficStep, count2));
                    trafficStep++;

                    road.updateTrafficLights();
                    feuStates.add(trafficLight.getState());

                    for (Vehicle v : new ArrayList<>(lane1.getVehicles())) {
                        v.bdiCycle(lane1, road);
                    }
                    for (Vehicle v : new ArrayList<>(lane2.getVehicles())) {
                        v.bdiCycle(lane2, road);
                    }

                    lane1.removeArrivedVehicles();
                    lane2.removeArrivedVehicles();
                    // ✅ Supprimer les véhicules qui ont atteint leur destination
                    vehicles.removeIf(v -> v.getBeliefs().contains("AtDestination", true));
                    lane1.getVehicles().removeIf(v -> v.getBeliefs().contains("AtDestination", true));
                    lane2.getVehicles().removeIf(v -> v.getBeliefs().contains("AtDestination", true));

                    if (vehicles.isEmpty()) {
                        System.out.println("✅ Tous les véhicules sont arrivés. Fin de la simulation.");
                        this.stop();
                        simulationTimer = null;
                    }
                    drawFrame();
                    lastUpdate = now;
                }

            }
        };
        simulationTimer.start();
    }

    private void showTrafficLightChart() {
        Stage chartStage = new Stage();
        chartStage.setTitle("Évolution des états du feu");

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Temps (étapes)");

        NumberAxis yAxis = new NumberAxis(0, 2, 1);
        yAxis.setLabel("État du feu");
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
            @Override
            public String toString(Number object) {
                return switch (object.intValue()) {
                    case 0 -> "ROUGE";
                    case 1 -> "ORANGE";
                    case 2 -> "VERT";
                    default -> "";
                };
            }
        });

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Évolution du feu de circulation");
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("État du feu");

        for (int i = 0; i < feuStates.size(); i++) {
            int value = switch (feuStates.get(i)) {
                case RED -> 0;
                case ORANGE -> 1;
                case GREEN -> 2;
            };
            series.getData().add(new XYChart.Data<>(i, value));
        }

        lineChart.getData().add(series);
        Scene chartScene = new Scene(lineChart, 600, 400);
        chartStage.setScene(chartScene);
        chartStage.show();
    }
    private void showCumulativeRewardChart() {
        Stage chartStage = new Stage();
        chartStage.setTitle("Récompenses cumulées par état");

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("État");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Valeur estimée");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Récompenses cumulées (MDP)");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Valeurs");

        // ✅ Accès dynamique depuis le feu de circulation
        Map<String, Double> stateRewards = trafficLight.getValueFunctionAsMap();
        for (Map.Entry<String, Double> entry : stateRewards.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        barChart.getData().add(series);

        Scene scene = new Scene(barChart, 800, 600);
        chartStage.setScene(scene);
        chartStage.show();
    }
    private void showVehicleInfoPopup(Vehicle v) {
        Stage popup = new Stage();
        popup.setTitle("🔎 Détails du véhicule ID: " + v.getId());

        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: #2b2b2b;");

        Label title = new Label("🚗 Informations Véhicule");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16;");

        Label mode = new Label("Mode : " + v.getMode());
        Label pos = new Label("📍 Position : " + v.getPosition());
        Label intentions = new Label("🧠 Intention actuelle : " +
                (v.getIntentions().peek() != null ? v.getIntentions().peek() : "Aucune"));

        Label desires = new Label("🔥 Désirs : " + v.getDesires());

        for (Label lbl : List.of(mode, pos, intentions, desires)) {
            lbl.setStyle("-fx-text-fill: lightgray;");
        }

        box.getChildren().addAll(title, mode, pos, intentions, desires);

        Scene scene = new Scene(box, 300, 200);
        popup.setScene(scene);
        popup.show();
    }
    private LineChart<Number, Number> createTrafficChart() {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Temps (étapes)");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Nombre de véhicules");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Évolution du trafic (lane1 vs lane2)");
        chart.setPrefWidth(500);
        chart.setAnimated(false);

        lane1Series = new XYChart.Series<>();
        lane1Series.setName("Voie 1");

        lane2Series = new XYChart.Series<>();
        lane2Series.setName("Voie 2");

        chart.getData().addAll(lane1Series, lane2Series);

        return chart;
    }







    public static void main(String[] args) {
        launch(args);
    }
}