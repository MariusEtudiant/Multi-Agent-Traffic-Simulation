import org.example.environment.TrafficLight;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TrafficLightTest {
    @Test
    public void testTrafficLightCreation() {
        TrafficLight trafficLight = new TrafficLight("IntersectionA", "GREEN");
        assertEquals("GREEN", trafficLight.getState());
    }

    @Test
    public void testSetState() {
        TrafficLight trafficLight = new TrafficLight("IntersectionA", "RED");
        trafficLight.setState("GREEN");
        assertEquals("GREEN", trafficLight.getState());
    }
}