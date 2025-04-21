package org.example.ArgumentationDM;

import org.tweetyproject.arg.dung.semantics.Extension;
import org.tweetyproject.arg.dung.syntax.Argument;
import org.tweetyproject.arg.dung.syntax.DungTheory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

public class DungGraphPanel extends JPanel {
    private final DungTheory theory;
    private final Set<Argument> accepted;

    // Argument ➜ mode
    private final Map<String, String> modeMap = Map.ofEntries(
            Map.entry("A13", "CAR"), Map.entry("A16", "CAR"), Map.entry("A17", "CAR"), Map.entry("A18", "CAR"),
            Map.entry("A3", "PUBLIC_TRANSPORT"), Map.entry("A10", "PUBLIC_TRANSPORT"),
            Map.entry("A22", "PUBLIC_TRANSPORT"), Map.entry("A25", "PUBLIC_TRANSPORT"),
            Map.entry("A5", "WALK"), Map.entry("A11", "WALK"), Map.entry("A14", "WALK"), Map.entry("A19", "WALK"),
            Map.entry("A7", "BIKE"), Map.entry("A12", "BIKE"), Map.entry("A21", "BIKE"), Map.entry("A23", "BIKE")
    );

    // Argument ➜ texte explicatif
    private final Map<String, String> labels = Map.ofEntries(
            Map.entry("A13", "Flexible"),
            Map.entry("A16", "Comfortable"),
            Map.entry("A17", "Long-term"),
            Map.entry("A18", "Time saver"),
            Map.entry("A3", "No driving"),
            Map.entry("A10", "Productive"),
            Map.entry("A22", "No parking"),
            Map.entry("A25", "Dense net"),
            Map.entry("A5", "Free"),
            Map.entry("A11", "Short dist."),
            Map.entry("A14", "Relaxing"),
            Map.entry("A19", "Healthy"),
            Map.entry("A7", "Fast city"),
            Map.entry("A12", "Avoid traffic"),
            Map.entry("A21", "Ecological"),
            Map.entry("A23", "Bike lanes"),
            Map.entry("A1", "Too expensive"),
            Map.entry("A2", "Cost justified"),
            Map.entry("A4", "Wait time"),
            Map.entry("A6", "Too slow"),
            Map.entry("A8", "Too tiring"),
            Map.entry("A9", "Traffic jam"),
            Map.entry("A24", "Pollution"),
            Map.entry("A26", "Overcrowded"),
            Map.entry("A27", "Good weather")

    );

    public DungGraphPanel(DungTheory theory, Collection<Argument> accepted) {
        this.theory = theory;
        this.accepted = new HashSet<>(accepted);
        setPreferredSize(new Dimension(1000, 800));
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int radius = 40;
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int circleRadius = Math.min(getWidth(), getHeight()) / 2 - 150;

        Map<Argument, Point> positions = new HashMap<>();
        List<Argument> args = new ArrayList<>(theory);
        int n = args.size();

        for (int i = 0; i < n; i++) {
            Argument arg = args.get(i);
            double angle = 2 * Math.PI * i / n;
            int x = (int) (centerX + circleRadius * Math.cos(angle));
            int y = (int) (centerY + circleRadius * Math.sin(angle));
            positions.put(arg, new Point(x, y));
        }

        // Flèches (attaques)
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(Color.BLACK);
        for (Argument from : theory) {
            for (Argument to : theory.getAttacked(from)) {
                drawArrow(g2, positions.get(from), positions.get(to));
            }
        }

        // Noeuds (arguments)
        for (Argument arg : args) {
            Point pos = positions.get(arg);
            String name = arg.getName();
            boolean isAccepted = accepted.contains(arg);
            String mode = modeMap.getOrDefault(name, "OTHER");
            String label = labels.getOrDefault(name, name);

            Color color = switch (mode) {
                case "CAR" -> new Color(66, 133, 244);
                case "PUBLIC_TRANSPORT" -> new Color(156, 39, 176);
                case "WALK" -> new Color(255, 215, 0);
                case "BIKE" -> new Color(255, 111, 0);
                default -> new Color(180, 180, 180); // au lieu de Color.GRAY
            };

            // Forme du nœud
            g2.setStroke(new BasicStroke(2.5f));
            g2.setColor(color);
            if (isAccepted) {
                g2.fillOval(pos.x - radius / 2, pos.y - radius / 2, radius, radius);
            } else {
                g2.drawOval(pos.x - radius / 2, pos.y - radius / 2, radius, radius);
            }

            // Contour noir
            g2.setColor(Color.BLACK);
            g2.drawOval(pos.x - radius / 2, pos.y - radius / 2, radius, radius);

            // Texte
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(label);
            g2.drawString(label, pos.x - textWidth / 2, pos.y + radius / 2 + 18); // au lieu de +15

        }

        drawLegend(g2);
    }

    private void drawArrow(Graphics2D g2, Point from, Point to) {
        double dx = to.x - from.x, dy = to.y - from.y;
        double angle = Math.atan2(dy, dx);
        int len = (int) Math.sqrt(dx * dx + dy * dy);

        AffineTransform old = g2.getTransform();
        g2.translate(from.x, from.y);
        g2.rotate(angle);

        g2.drawLine(0, 0, len - 10, 0);

        Polygon arrowHead = new Polygon();
        arrowHead.addPoint(0, 0);
        arrowHead.addPoint(-6, -6);
        arrowHead.addPoint(-6, 6);
        g2.fillPolygon(arrowHead);

        g2.setTransform(old);
    }

    private void drawLegend(Graphics2D g2) {
        int x = getWidth() - 200;
        int y = getHeight() - 180;
        int boxSize = 12;

        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        g2.setColor(Color.BLACK);
        g2.drawString("● = Accepté     ○ = Rejeté", x, y);
        y += 20;

        Map<String, Color> legendColors = Map.of(
                "CAR", new Color(66, 133, 244),
                "PT", new Color(156, 39, 176),
                "WALK", new Color(255, 215, 0),
                "BIKE", new Color(255, 111, 0)
        );

        for (Map.Entry<String, Color> entry : legendColors.entrySet()) {
            g2.setColor(entry.getValue());
            g2.fillRect(x, y, boxSize, boxSize);
            g2.setColor(Color.BLACK);
            g2.drawString(entry.getKey(), x + 20, y + boxSize);
            y += 20;
        }
    }

    // Export
    public static class GraphExporter {
        public static void exportPanelAsPNG(JPanel panel, String filename) {
            BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
            panel.paint(image.getGraphics());
            try {
                ImageIO.write(image, "png", new File(filename));
                System.out.println("✅ Graph exporté : " + filename);
            } catch (Exception e) {
                System.err.println("❌ Échec export PNG : " + e.getMessage());
            }
        }
    }
}
