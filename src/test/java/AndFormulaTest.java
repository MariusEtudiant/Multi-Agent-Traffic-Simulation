

import org.example.agent.Belief;
import org.example.agent.BeliefInitial;
import org.example.logic.AndFormula;
import org.example.logic.AtomFormula;
import org.example.logic.LogicalFormula;
import org.example.logic.NotFormula;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AndFormulaTest {
    @Test
    public void testEvaluate() {
        BeliefInitial beliefs = new BeliefInitial();
        beliefs.addBelief(new Belief("FeuVert", true));
        beliefs.addBelief(new Belief("CarAhead", false));

        LogicalFormula formula1 = b -> b.contains("FeuVert", true);
        LogicalFormula formula2 = b -> b.contains("CarAhead", false);

        AndFormula andFormula = new AndFormula(formula1, formula2);
        assertTrue(andFormula.evaluate(beliefs));
    }

    @Test
    public void testLogicalFormulas() {
        BeliefInitial beliefs = new BeliefInitial();
        beliefs.addBelief(new Belief("FeuVert", true));
        beliefs.addBelief(new Belief("CarAhead", false));

        LogicalFormula feuVert = new AtomFormula("FeuVert", true);
        LogicalFormula carAhead = new AtomFormula("CarAhead", true);
        LogicalFormula canAccelerate = new AndFormula(feuVert, new NotFormula(carAhead));

        assertTrue(canAccelerate.evaluate(beliefs));
    }
}