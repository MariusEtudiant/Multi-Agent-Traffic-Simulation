

import org.example.agent.Intention;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IntentionTest {
    @Test
    public void testIntentionValues() {
        assertEquals(Intention.ACCELERATE, Intention.valueOf("ACCELERATE"));
        assertEquals(Intention.TURN_LEFT, Intention.valueOf("TURN_LEFT"));
    }
}