package org.example.logic;

import org.example.agent.BeliefInitial;

public class NotFormula implements LogicalFormula {
    private LogicalFormula formula;

    public NotFormula(LogicalFormula formula) {
        this.formula = formula;
    }

    @Override
    public boolean evaluate(BeliefInitial beliefs){
        return !formula.evaluate(beliefs);
    }
}
