package org.example.logic;

import org.example.agent.BeliefInitial;

public class AtomFormula implements LogicalFormula{
    private String name;
    private Object value;

    public AtomFormula(String name, Object value){
        this.name = name;
        this.value = value;
    }
    @Override
    public boolean evaluate(BeliefInitial beliefs){
        return beliefs.contains(name, value);
    }
}
