// TransportationAgent.java
package org.example.ArgumentationDM;

import org.tweetyproject.arg.dung.reasoner.SimpleCompleteReasoner;
import org.tweetyproject.arg.dung.reasoner.SimpleGroundedReasoner;
import org.tweetyproject.arg.dung.reasoner.SimplePreferredReasoner;
import org.tweetyproject.arg.dung.reasoner.SimpleStableReasoner;
import org.tweetyproject.arg.dung.semantics.Extension;
import org.tweetyproject.arg.dung.syntax.Argument;
import org.tweetyproject.arg.dung.syntax.DungTheory;

import java.util.*;

public class TransportationAgent {
    private final double distance;
    private final String weather;
    private final boolean isHealthy;
    private final boolean isRushHour;

    public TransportationAgent(double distance, String weather, boolean isHealthy, boolean isRushHour) {
        this.distance = distance;
        this.weather = weather;
        this.isHealthy = isHealthy;
        this.isRushHour = isRushHour;
    }

    public String decideTransportationMode() {
        DungTheory theory = getFramework();
        SimpleGroundedReasoner reasoner = new SimpleGroundedReasoner();
        Extension accepted = reasoner.getModel(theory);

        Map<String, String> modes = Map.of(
                "A1", "CAR",
                "A3", "PUBLIC_TRANSPORT",
                "A5", "WALK",
                "A7", "BIKE",
                "A10", "PUBLIC_TRANSPORT",
                "A11", "WALK",
                "A12", "BIKE"
        );

        Map<String, Integer> scores = new HashMap<>();
        for (Object obj : accepted) {
            if (obj instanceof Argument arg) {
                String mode = modes.get(arg.getName());
                if (mode != null) {
                    scores.put(mode, scores.getOrDefault(mode, 0) + 1);
                }
            }
        }

        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");
    }

    public DungTheory getFramework() {
        DungTheory theory = new DungTheory();

        Argument A1 = new Argument("A1"); theory.add(A1);
        Argument A2 = new Argument("A2"); theory.add(A2); theory.addAttack(A2, A1);

        Argument A3 = new Argument("A3"); theory.add(A3);
        Argument A4 = new Argument("A4"); theory.add(A4); theory.addAttack(A4, A3);

        Argument A5 = new Argument("A5"); theory.add(A5);
        Argument A6 = new Argument("A6"); theory.add(A6); theory.addAttack(A6, A5);

        Argument A7 = new Argument("A7"); theory.add(A7);
        Argument A8 = new Argument("A8"); theory.add(A8); theory.addAttack(A8, A7);

        if (distance > 5.0) theory.addAttack(A6, A5);
        if (weather.equalsIgnoreCase("rainy")) theory.addAttack(A8, A7);
        if (isRushHour) {
            Argument A9 = new Argument("A9");
            theory.add(A9);
            theory.addAttack(A9, A1);
        }

        // Contre-attaques (défenses)
        if (!isRushHour) {
            Argument A10 = new Argument("A10"); theory.add(A10); theory.addAttack(A10, A4);
        }
        if (distance <= 3.0) {
            Argument A11 = new Argument("A11"); theory.add(A11); theory.addAttack(A11, A6);
        }
        if (!weather.equalsIgnoreCase("rainy")) {
            Argument A12 = new Argument("A12"); theory.add(A12); theory.addAttack(A12, A8);
        }

        return theory;
    }
    public Map<String, Extension> compareSemantics() {
        Map<String, Extension> result = new LinkedHashMap<>();
        DungTheory theory = getFramework();

        result.put("Grounded", new SimpleGroundedReasoner().getModel(theory));
        result.put("Preferred", new SimplePreferredReasoner().getModel(theory));
        result.put("Complete", new SimpleCompleteReasoner().getModel(theory));
        result.put("Stable", new SimpleStableReasoner().getModel(theory));

        return result;
    }

}