package org.example.agent;

import java.util.Objects;

public class Belief {
    private String name;
    private Object value;

    public Belief(String name, Object value) {
        this.name = name;
        this.value = value;
    }
    public String getName() {
        return name;
    }
    public Object getValue() {
        return value;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Belief belief = (Belief) o;
        return Objects.equals(name, belief.name) && Objects.equals(value, belief.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }
}
