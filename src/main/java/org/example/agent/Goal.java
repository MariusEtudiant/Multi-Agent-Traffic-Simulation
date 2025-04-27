/*
Better management of objectives based on a desire and its modal logic of success, greatly simplifies processing
 */

package org.example.agent;

import org.example.logic.LogicalFormula;

public class Goal {
    private Desire desire;
    private LogicalFormula successCondition;

    public Goal(Desire desire, LogicalFormula successCondition) {
        this.desire = desire;
        this.successCondition = successCondition;
    }

    public boolean isAchieved(BeliefInitial beliefs) {
        return successCondition.evaluate(beliefs);
    }
    public Desire getDesire() {
        return desire;
    }
}