/*
 * TransportationAgent.java
 * -------------------------
 * Agent de choix du mode de transport via argumentation (Tweety).
 * Utilise la mesure scr(d) = (pros - 0.5 * cons) / (pros + cons) + mini-prior,
 * puis normalise pour obtenir des pourcentages.
 */
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

    // ===== Contexte de l'agent (entrée) =====
    private final Position start;
    private final Position destination;
    private final String weather;       // "Sunny", "Cloudy", "Rainy"
    private final boolean isHealthy;
    private final boolean isRushHour;

    // ===== Modes disponibles =====
    private static final List<String> MODES = List.of(
            "CAR", "PUBLIC_TRANSPORT", "WALK", "BIKE"
    );

    /**
     * Initialisation de l'agent avec son contexte.
     */
    public TransportationAgent(
            Position start,
            Position destination,
            String weather,
            boolean isHealthy,
            boolean isRushHour
    ) {
        this.start = start;
        this.destination = destination;
        this.weather = weather;
        this.isHealthy = isHealthy;
        this.isRushHour = isRushHour;
    }

    /**
     * Décide du meilleur mode selon la méthode SCR-based.
     * @return le nom du mode sélectionné.
     */
    public String decideTransportationMode() {
        return getModeScoresScr().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("CAR");
    }

    /**
     * Calcule pour chaque mode un score SCR, puis ajoute un mini-prior
     * et normalise pour renvoyer un pourcentage.
     * @return Map<Mode, Pourcentage>
     */
    public Map<String, Double> getModeScoresScr() {
        // 1) Construction du cadre argumentatif
        DungTheory framework = buildFramework();

        // 2) Cartographie des arguments pros/cons par mode
        Map<String, String> posArgs = Map.ofEntries(
                // Arguments favorables
                Map.entry("A13", "CAR"), Map.entry("A18", "CAR"),
                Map.entry("A28", "CAR"), Map.entry("A30", "CAR"),
                Map.entry("A60", "CAR"), Map.entry("A61", "CAR"),
                Map.entry("A62", "CAR"), Map.entry("A63", "CAR"), Map.entry("A64", "CAR"),
                Map.entry("A3", "PUBLIC_TRANSPORT"),
                Map.entry("A22", "PUBLIC_TRANSPORT"), Map.entry("A25", "PUBLIC_TRANSPORT"),
                Map.entry("A5", "WALK"), Map.entry("A11", "WALK"),
                Map.entry("A14", "WALK"), Map.entry("A19", "WALK"),
                Map.entry("A7", "BIKE"), Map.entry("A12", "BIKE"),
                Map.entry("A21", "BIKE"), Map.entry("A23", "BIKE")
        );
        Map<String, String> negArgs = Map.ofEntries(
                // Arguments défavorables
                Map.entry("A1", "CAR"), Map.entry("A24", "CAR"), Map.entry("A9", "CAR"),
                Map.entry("A4", "PUBLIC_TRANSPORT"), Map.entry("A26", "PUBLIC_TRANSPORT"),
                Map.entry("A31", "PUBLIC_TRANSPORT"), Map.entry("A40", "PUBLIC_TRANSPORT"),
                Map.entry("A6", "WALK"), Map.entry("A50", "WALK"),
                Map.entry("A8", "BIKE"), Map.entry("A35", "BIKE"), Map.entry("A36", "BIKE"),Map.entry("A37", "BIKE") // Nouvel argument (ex: risque d'accident)
        );

        // 3) Préparation des raisonners (4 sémantiques)
        List<AbstractExtensionReasoner> reasoners = List.of(
                new SimpleGroundedReasoner(),
                new SimplePreferredReasoner(),
                new SimpleCompleteReasoner(),
                new SimpleStableReasoner()
        );

        // 4) Accumulation des scores raw
        Map<String, Double> raw = MODES.stream()
                .collect(Collectors.toMap(Function.identity(), m -> 0.0));
        for (AbstractExtensionReasoner r : reasoners) {
            Extension ext = r.getModel(framework);
            Map<String, Long> pros = countAccepted(ext, posArgs);
            Map<String, Long> cons = countAccepted(ext, negArgs);
            for (String mode : MODES) {
                long p = pros.getOrDefault(mode, 0L);
                long c = cons.getOrDefault(mode, 0L);
                if (p + c > 0) {
                    // formule scr ajustée
                    double scr = (p - 0.5 * c) / (p + c);
                    raw.merge(mode, scr, Double::sum);
                }
            }
        }

        // 5) Moyenne sur les 4 sémantiques
        int n = reasoners.size();

        // 6) Ajout d'un mini-prior pour débloquer modes
        Map<String, Double> prior = Map.of(
                "CAR", 0.40,          // Augmenté de 30% à 40%
                "PUBLIC_TRANSPORT", 0.20, // Réduit de 25% à 15%
                "WALK", 0.05,
                "BIKE", 0.08
        );

        // 7) Combinaison et normalisation en %
        Map<String, Double> combined = new LinkedHashMap<>();
        double total = 0.0;
        for (String mode : MODES) {
            double average = raw.get(mode) / n;
            double score = average + prior.get(mode);
            combined.put(mode, score);
            total += score;
        }
        Map<String, Double> finalScores = new LinkedHashMap<>();
        for (String mode : MODES) {
            finalScores.put(mode, round2(combined.get(mode) / total * 100));
        }
        return finalScores;
    }

    /**
     * Construit le graphe d'arguments selon le contexte (attaques conditionnelles).
     */
    /* ======= Graphe ======= */
    public DungTheory buildFramework() {
        DungTheory th = new DungTheory();
        Map<String, Argument> A = new HashMap<>();
        Function<String, Argument> arg = n -> A.computeIfAbsent(n, x -> {
            Argument a = new Argument(x);
            th.add(a);
            return a;
        });

        // === Arguments & attaques de base ===

        // --- CAR ---
        th.addAttack(arg.apply("A28"), arg.apply("A1"));    // Disponible vs coût
        th.addAttack(arg.apply("A13"), arg.apply("A2"));    // Confort défend coût
        th.addAttack(arg.apply("A18"), arg.apply("A2"));    // Vitesse défend coût
        th.addAttack(arg.apply("A30"), arg.apply("A5"));    // Familles vs marche
        th.addAttack(arg.apply("A24"), arg.apply("A13"));   // Pollution attaque confort
        th.addAttack(arg.apply("A62"), arg.apply("A6"));    // Transport charge vs marche
        th.addAttack(arg.apply("A63"), arg.apply("A3"));    // Long trajet vs PT
        th.addAttack(arg.apply("A64"), arg.apply("A36"));   // Confort voiture vs vélo pluie
        th.addAttack(arg.apply("A18"), arg.apply("A26"));


        // --- PUBLIC TRANSPORT ---
        th.addAttack(arg.apply("A4"), arg.apply("A3"));      // Attente vs PT
        th.addAttack(arg.apply("A22"), arg.apply("A1"));    // Pas besoin de parking vs voiture
        th.addAttack(arg.apply("A25"), arg.apply("A4"));    // Réseau compense attente


        // --- WALK ---
        th.addAttack(arg.apply("A6"), arg.apply("A5"));      // Trop lent
        th.addAttack(arg.apply("A11"), arg.apply("A6"));     // Ok sur courte distance
        th.addAttack(arg.apply("A14"), arg.apply("A6"));     // Relaxant
        if (!isHealthy)
            th.addAttack(arg.apply("A33"), arg.apply("A5")); // Marche trop fatigante si santé faible
        if (start.distanceTo(destination) > 50)
            th.addAttack(arg.apply("A6"), arg.apply("A5"));  // Trop long
        if (start.distanceTo(destination) > 70)
            th.addAttack(arg.apply("A50"), arg.apply("A5")); // Encore plus trop long

        // --- BIKE ---
        th.addAttack(arg.apply("A8"), arg.apply("A7"));      // Effort
        th.addAttack(arg.apply("A12"), arg.apply("A8"));     // Évite trafic
        th.addAttack(arg.apply("A21"), arg.apply("A8"));     // Écologique
        th.addAttack(arg.apply("A23"), arg.apply("A8"));     // Pistes cyclables
        if (!isHealthy)
            th.addAttack(arg.apply("A35"), arg.apply("A7")); // Pas assez en forme

        // === Conditions contextuelles regroupées ===

        if (isRushHour) {
            // 🚗 Voiture
            th.addAttack(arg.apply("A9"), arg.apply("A18"));     // Bouchons

            // 🚆 Public Transport
            th.addAttack(arg.apply("A26"), arg.apply("A3"));     // Surcharge
            th.addAttack(arg.apply("A40"), arg.apply("A3"));     // Inconfort
            th.addAttack(arg.apply("A41"), arg.apply("A25"));    // Réseau saturé
        }

        if (weather.equalsIgnoreCase("Rainy")) {
            // 🌧️ Pluie affecte les transports publics et vélo
            th.addAttack(arg.apply("A31"), arg.apply("A3"));     // Moins fiable
            th.addAttack(arg.apply("A60"), arg.apply("A31"));    // Voiture au sec
            th.addAttack(arg.apply("A61"), arg.apply("A36"));    // Plus sûre que le vélo
            th.addAttack(arg.apply("A36"), arg.apply("A7"));     // Dangereux vélo
            th.addAttack(arg.apply("A64"), arg.apply("A7"));  // Confort voiture attaque rapidité vélo
        }

        return th;
    }

    // ===== Utilitaires =====
    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static Map<String, Long> countAccepted(Extension ext, Map<String, String> map) {
        Map<String, Long> res = new HashMap<>();
        for (Object obj : ext) {
            Argument a = (Argument) obj;
            String mode = map.get(a.getName());
            if (mode != null) res.merge(mode, 1L, Long::sum);
        }
        return res;
    }
    public Map<String,Extension> compareSemantics(){
        DungTheory t=buildFramework();
        return Map.of("Grounded",new SimpleGroundedReasoner().getModel(t),
                "Preferred",new SimplePreferredReasoner().getModel(t),
                "Complete",new SimpleCompleteReasoner().getModel(t),
                "Stable",new SimpleStableReasoner().getModel(t));
    }
    public DungTheory getFramework(){return buildFramework();}
}