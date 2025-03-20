import org.example.agent.Belief;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BeliefTest {
    @Test
    public void testBeliefCreation() {
        Belief belief = new Belief("FeuVert", true);
        assertEquals("FeuVert", belief.getName());
        assertEquals(true, belief.getValue());
    }
}