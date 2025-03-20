

import org.example.agent.Belief;
import org.example.agent.BeliefInitial;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BeliefInitialTest {
    @Test
    public void testAddBelief() {
        BeliefInitial beliefInitial = new BeliefInitial();
        beliefInitial.addBelief(new Belief("FeuVert", true));
        assertTrue(beliefInitial.contains("FeuVert", true));
    }

    @Test
    public void testContainsBelief() {
        BeliefInitial beliefInitial = new BeliefInitial();
        beliefInitial.addBelief(new Belief("FeuRouge", false));
        assertFalse(beliefInitial.contains("FeuVert", true));
    }
}