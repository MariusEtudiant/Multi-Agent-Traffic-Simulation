// TransportationAgent.java
package org.example.ArgumentationDM;

import org.example.agent.Position;
import org.tweetyproject.arg.dung.reasoner.*;
import org.tweetyproject.arg.dung.semantics.Extension;
import org.tweetyproject.arg.dung.syntax.Argument;
import org.tweetyproject.arg.dung.syntax.DungTheory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TransportationAgent {

    /* ============== Variables d’entrée ============== */
    private final Position start, destination;
    private final String weather;
    private final boolean isHealthy, isRushHour;

    /* ============== Paramètres globaux ============== */
    private static final double SOFTMAX_TEMPERATURE = 1.0;   // ↓ adoucir, ↑ accentuer
    private static final List<String> MODES = List.of("CAR", "PUBLIC_TRANSPORT", "WALK", "BIKE");

    /* ------------------------------------------------ */
    public TransportationAgent(Position start,
                               Position destination,
                               String weather,
                               boolean isHealthy,
                               boolean isRushHour) {
        this.start = start;
        this.destination = destination;
        this.weather = weather;
        this.isHealthy = isHealthy;
        this.isRushHour = isRushHour;
    }

    /* =====================================================
     *  1.  Décision « pratique » : mode le plus probable
     * ==================================================== */
    public String decideTransportationMode() {
        Map<String, Double> probs = getModeProbabilities();
        return probs.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("CAR");
    }

    /* =====================================================
     *  2.  Distribution de probabilités
     * ==================================================== */
    public Map<String, Double> getModeProbabilities() {
        DungTheory framework = buildFramework();

        /* ----- Cartographie argument ➜ (mode, +/−) ----------------- */
        Map<String, String> posArgToMode = Map.ofEntries(
                // CAR
                Map.entry("A13","CAR"), Map.entry("A16","CAR"),
                Map.entry("A17","CAR"), Map.entry("A18","CAR"),
                // PT
                Map.entry("A3", "PUBLIC_TRANSPORT"), Map.entry("A10","PUBLIC_TRANSPORT"),
                Map.entry("A22","PUBLIC_TRANSPORT"), Map.entry("A25","PUBLIC_TRANSPORT"),
                // WALK
                Map.entry("A5","WALK"), Map.entry("A11","WALK"),
                Map.entry("A14","WALK"), Map.entry("A19","WALK"),
                // BIKE
                Map.entry("A7","BIKE"), Map.entry("A12","BIKE"),
                Map.entry("A21","BIKE"), Map.entry("A23","BIKE")
        );

        Map<String, String> negArgToMode = Map.ofEntries(
                // CAR
                Map.entry("A1","CAR"),  Map.entry("A24","CAR"),
                // PT
                Map.entry("A4","PUBLIC_TRANSPORT"), Map.entry("A26","PUBLIC_TRANSPORT"),
                // WALK
                Map.entry("A6","WALK"),
                // BIKE
                Map.entry("A8","BIKE")
        );

        /* ----- Ensemble des sémantiques à agréger ---------- */
        List<AbstractExtensionReasoner> reasoners = List.of(
                new SimpleGroundedReasoner(),
                new SimplePreferredReasoner(),
                new SimpleCompleteReasoner(),
                new SimpleStableReasoner()
        );

        /* ----- Initialisation score cumulatif par mode ----- */
        Map<String, Double> cumulative = MODES.stream()
                .collect(Collectors.toMap(Function.identity(), x -> 0.0));

        /* ----- Calcul pour chaque sémantique --------------- */
        for (AbstractExtensionReasoner reasoner : reasoners) {
            Extension ext = reasoner.getModel(framework);

            Map<String, Long> posTotal = countTotal(posArgToMode);
            Map<String, Long> negTotal = countTotal(negArgToMode);

            Map<String, Long> posAccepted = countAccepted(ext, posArgToMode);
            Map<String, Long> negAccepted = countAccepted(ext, negArgToMode);

            for (String mode : MODES) {
                long posCount       = posAccepted.getOrDefault(mode, 0L);
                long posTotalCount  = posTotal.getOrDefault(mode, 0L);
                long negCount       = negAccepted.getOrDefault(mode, 0L);
                long negTotalCount  = negTotal.getOrDefault(mode, 0L);

                double posRatio = safeRatio(posCount, posTotalCount);
                double negRatio = safeRatio(negCount, negTotalCount);

                double score = posRatio - negRatio; // ∈ [-1,1]
                cumulative.merge(mode, score, Double::sum);
            }

        }

        /* ----- Moyenne et soft‑max pour obtenir des % ------- */
        double min = Collections.min(cumulative.values());
        // Shift pour éviter les exponentielles trop petites
        Map<String, Double> exp = new HashMap<>();
        double sumExp = 0.0;
        for (String mode : MODES) {
            double val = (cumulative.get(mode) - min) / reasoners.size(); // normalise [-1,1] → [0,2]
            double e   = Math.exp(val / SOFTMAX_TEMPERATURE);
            exp.put(mode, e);
            sumExp += e;
        }

        Map<String, Double> probs = new LinkedHashMap<>();
        for (String mode : MODES) {
            probs.put(mode, round2(exp.get(mode) / sumExp * 100)); // pourcentage
        }
        return probs;
    }

    /* =====================================================
     *  3.  Construction dynamique du DungFramework
     * ==================================================== */
    private DungTheory buildFramework() {
        DungTheory th = new DungTheory();

        /* utilitaire interne */
        Map<String, Argument> A = new HashMap<>();
        java.util.function.Function<String, Argument> arg = name -> A.computeIfAbsent(name, n -> {
            Argument a = new Argument(n);
            th.add(a);
            return a;
        });

        /* === Arguments & attaques ======================== */
        // -------- CAR --------
        th.addAttack(arg.apply("A2"),  arg.apply("A1"));   // A1 : coût ↑ ; A2 réfute
        th.addAttack(arg.apply("A13"), arg.apply("A2"));
        th.addAttack(arg.apply("A16"), arg.apply("A2"));
        th.addAttack(arg.apply("A17"), arg.apply("A2"));
        th.addAttack(arg.apply("A18"), arg.apply("A2"));
        // Pollution
        th.addAttack(arg.apply("A24"), arg.apply("A13"));
        th.addAttack(arg.apply("A24"), arg.apply("A16"));
        th.addAttack(arg.apply("A24"), arg.apply("A18"));
        // Embouteillages
        if (isRushHour) {
            th.addAttack(arg.apply("A9"), arg.apply("A18"));
        }

        // -------- PUBLIC TRANSPORT --------
        th.addAttack(arg.apply("A4"),  arg.apply("A3"));    // attente
        th.addAttack(arg.apply("A10"), arg.apply("A4"));    // travail/lecture
        th.addAttack(arg.apply("A22"), arg.apply("A1"));    // pas de parking
        th.addAttack(arg.apply("A25"), arg.apply("A4"));    // réseau dense
        if (isRushHour) {
            th.addAttack(arg.apply("A26"), arg.apply("A3")); // sur‑chargé
        }

        // -------- WALK --------
        th.addAttack(arg.apply("A6"),  arg.apply("A5"));   // lent
        th.addAttack(arg.apply("A11"), arg.apply("A6"));   // distance courte
        th.addAttack(arg.apply("A14"), arg.apply("A6"));   // relaxant
        if (isHealthy) {
            th.addAttack(arg.apply("A19"), arg.apply("A6")); // santé
        }
        double dist = start.distanceTo(destination);
        if (dist > 50)  th.addAttack(arg.apply("A6"), arg.apply("A5"));

        // -------- BIKE --------
        th.addAttack(arg.apply("A8"),  arg.apply("A7"));   // effort
        th.addAttack(arg.apply("A12"), arg.apply("A8"));   // évite trafic
        th.addAttack(arg.apply("A21"), arg.apply("A8"));   // climat
        th.addAttack(arg.apply("A23"), arg.apply("A8"));   // pistes
        if ("rainy".equalsIgnoreCase(weather)) {
            th.addAttack(arg.apply("A8"), arg.apply("A7")); // pluie accentue négatif
        } else {
            th.addAttack(arg.apply("A27"), arg.apply("A8")); // météo clémente
        }
        return th;
    }

    /* =====================================================
     *                 Méthodes utilitaires
     * ==================================================== */
    private static Map<String, Long> countTotal(Map<String, String> map) {
        return map.values().stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }
    private static Map<String, Long> countAccepted(Extension ext, Map<String, String> map) {
        Map<String, Long> res = new HashMap<>();
        for (Argument a : (Collection<Argument>) ext) {
            String mode = map.get(a.getName());
            if (mode != null) res.merge(mode, 1L, Long::sum);
        }
        return res;
    }
    private static double safeRatio(long num, long den) {
        return den == 0 ? 0.0 : (double) num / den;
    }
    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /* =====================================================
     *  4.  (Optionnel) Inspection des différentes sémantiques
     * ==================================================== */
    public Map<String, Extension> compareSemantics() {
        DungTheory th = buildFramework();
        Map<String, Extension> r = new LinkedHashMap<>();
        r.put("Grounded",  new SimpleGroundedReasoner().getModel(th));
        r.put("Preferred", new SimplePreferredReasoner().getModel(th));
        r.put("Complete",  new SimpleCompleteReasoner().getModel(th));
        r.put("Stable",    new SimpleStableReasoner().getModel(th));
        return r;
    }
    public DungTheory getFramework() {
        return buildFramework();
    }

}
