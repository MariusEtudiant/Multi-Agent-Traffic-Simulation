package org.example.logic;

import org.example.agent.BeliefInitial;

public interface LogicalFormula {
    boolean evaluate(BeliefInitial beliefs);
}




