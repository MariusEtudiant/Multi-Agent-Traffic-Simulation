package org.example.logic;

import org.example.agent.BeliefInitial;

public class AndFormula implements LogicalFormula {
    private LogicalFormula left;
    private LogicalFormula right;

    public AndFormula(LogicalFormula left, LogicalFormula right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean evaluate(BeliefInitial beliefs) {
        return left.evaluate(beliefs) && right.evaluate(beliefs);
    }
}