import org.example.agent.Desire;
import org.example.agent.Intention;
import org.example.agent.Position;
import org.example.agent.Vehicle;
import org.example.environment.Environment;
import org.example.environment.TrafficLight;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class VehicleTest {
    @Test
    public void testAddDesire() {
        Vehicle vehicle = new Vehicle(new Position(0, 0));
        vehicle.addDesire(new Desire("ReachDestination"));
        assertEquals(1, vehicle.getDesires().size());
    }

    @Test
    public void testPerceiveEnvironment() {
        Vehicle vehicle = new Vehicle(new Position(0, 0));
        Environment env = new Environment();
        // Simuler un feu vert à l'intersection A
        env.addTrafficLight(new TrafficLight("IntersectionA", "GREEN"));

        // Simuler un véhicule devant
        Vehicle otherVehicle = new Vehicle(new Position(5, 5));
        env.addVehicle(otherVehicle);

        vehicle.perceivedEnvironment(env);
        // Vérifier que les croyances sont mises à jour
        assertTrue(vehicle.getBeliefs().contains("FeuVert", true));
        assertTrue(vehicle.getBeliefs().contains("CarAhead", true));
    }

    @Test
    public void testCollisionAvoidance() {
        Environment env = new Environment();
        Vehicle vehicle1 = new Vehicle(new Position(0, 0));
        Vehicle vehicle2 = new Vehicle(new Position(1, 1));
        env.addVehicle(vehicle1);
        env.addVehicle(vehicle2);

        vehicle1.decideNextAction(env);
        assertTrue(vehicle1.getIntentions().contains(Intention.SLOW_DOWN));
    }

    @Test
    public void testIsTooClose() {
        Vehicle vehicle1 = new Vehicle(new Position(0, 0));
        Vehicle vehicle2 = new Vehicle(new Position(5, 5));

        assertTrue(vehicle1.isTooClose(vehicle2)); // Distance ~7.07 < SAFE_DISTANCE (10)
    }

    @Test
    public void testGetIntentions() {
        Vehicle vehicle = new Vehicle(new Position(0, 0));
        vehicle.addIntention(Intention.ACCELERATE);

        assertEquals(1, vehicle.getIntentions().size());
        assertTrue(vehicle.getIntentions().contains(Intention.ACCELERATE));
    }
}