

import org.example.agent.Position;
import org.example.agent.Vehicle;
import org.example.environment.Environment;
import org.example.environment.TrafficLight;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EnvironmentTest {
    @Test
    public void testAddVehicle() {
        Environment env = new Environment();
        Vehicle vehicle = new Vehicle(new Position(0,0));
        env.addVehicle(vehicle);
        assertEquals(1, env.getVehicles().size());
    }

    @Test
    public void testAddTrafficLight() {
        Environment env = new Environment();
        TrafficLight trafficLight = new TrafficLight("IntersectionA", "GREEN");
        env.addTrafficLight(trafficLight);
        assertEquals(1, env.getTrafficLights().size());
    }
}