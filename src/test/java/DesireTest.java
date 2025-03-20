

import org.example.agent.Desire;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DesireTest {
    @Test
    public void testDesireCreation() {
        Desire desire = new Desire("ReachDestination");
        assertEquals("ReachDestination", desire.getName());
        assertFalse(desire.isAchieved());
    }

    @Test
    public void testSetAchieved() {
        Desire desire = new Desire("AvoidCollision");
        desire.setAchieved(true);
        assertTrue(desire.isAchieved());
    }
}