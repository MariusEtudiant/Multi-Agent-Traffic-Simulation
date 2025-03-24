package org.example;

import org.example.agent.Vehicle;
import org.example.environment.Environment;
import org.example.environment.Obstacle;
import org.example.environment.Road;
import org.example.environment.TrafficLight;
import org.example.agent.Position;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Créer l'environnement
        Environment env = new Environment();

        // Création d'une route de 100m de long avec une capacité de 10 véhicules
        List<Position> entryPoints = List.of(new Position(0, 0), new Position(100, 0));
        Road road = new Road("R1", 100.0, 10, entryPoints);

        // Ajouter un feu de circulation
        TrafficLight trafficLight = new TrafficLight("Road1", "GREEN");
        road.addTrafficLight(trafficLight, new Position(95,0));

        // Ajouter des véhicules
        Vehicle vehicle1 = new Vehicle(new Position(0, 0));
        Vehicle vehicle2 = new Vehicle(new Position(15, 0));
        road.addVehicle(vehicle1);
        road.addVehicle(vehicle2);

        Obstacle obstacle1 = new Obstacle(new Position(70,0));
        road.addObstacle(obstacle1);

        System.out.println(road);  // "Road R1 (Length: 100.0, Vehicles: 2/10)"
        System.out.println("Congested? " + road.isCongested());  // false

        for (int i = 0; i < 10; i++) {
            System.out.println("=== Étape " + (i + 1) + " ===");

            // Alterner l'état des feux toutes les 2 étapes
            if (i % 2 == 0) {
                for (TrafficLight trafficLights : road.getTrafficLights()) {
                    trafficLights.toggleState();
                    System.out.println("Le feu de circulation " + trafficLights.getId() + " est maintenant " + trafficLights.getState());
                }
            }

            // Mettre à jour l'environnement
            road.update();

            // Afficher l'état des véhicules
            System.out.println("Véhicule 1 :");
            System.out.println(" - Position : " + vehicle1.getPosition().getX() + ", " + vehicle1.getPosition().getY());
            System.out.println(" - Intentions : " + vehicle1.getIntentions());
            System.out.println(" - Croyances : " + vehicle1.getBeliefs());

            System.out.println("Véhicule 2 :");
            System.out.println(" - Position : " + vehicle2.getPosition().getX() + ", " + vehicle2.getPosition().getY());
            System.out.println(" - Intentions : " + vehicle2.getIntentions());
            System.out.println(" - Croyances : " + vehicle2.getBeliefs());

            // Faire agir les véhicules
            vehicle1.act();
            vehicle2.act();

            // Attendre un peu pour simuler le temps réel
            try {
                Thread.sleep(1000); // 1 seconde entre chaque étape
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}