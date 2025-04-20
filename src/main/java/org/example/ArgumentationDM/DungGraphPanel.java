package org.example.ArgumentationDM;

import org.tweetyproject.arg.dung.semantics.Extension;
import org.tweetyproject.arg.dung.syntax.Argument;
import org.tweetyproject.arg.dung.syntax.DungTheory;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class DungGraphPanel extends JPanel {
    private final DungTheory theory;
    private final Set<Argument> accepted;

    public DungGraphPanel(DungTheory theory, Collection<Argument> accepted) {
        this.theory = theory;
        this.accepted = new HashSet<>(accepted);
        setPreferredSize(new Dimension(800, 600));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        int radius = 40;
        int margin = 100;
        int i = 0;

        Map<Argument, Point> positions = new HashMap<>();

        // Placement circulaire
        for (Argument arg : theory) {
            double angle = 2 * Math.PI * i / theory.size();
            int x = (int) (getWidth() / 2 + 200 * Math.cos(angle));
            int y = (int) (getHeight() / 2 + 200 * Math.sin(angle));
            positions.put(arg, new Point(x, y));
            i++;
        }

        // Dessiner les flèches (attaques)
        g2.setColor(Color.BLACK);
        for (Argument from : theory) {
            for (Argument to : theory.getAttacked(from)) {
                Point p1 = positions.get(from);
                Point p2 = positions.get(to);
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }

        // Dessiner les nœuds
        for (Argument arg : theory) {
            Point pos = positions.get(arg);
            g2.setColor(accepted.contains(arg) ? Color.GREEN : Color.RED);
            g2.fillOval(pos.x - radius / 2, pos.y - radius / 2, radius, radius);
            g2.setColor(Color.BLACK);
            g2.drawString(arg.getName(), pos.x - 10, pos.y + 5);
        }
    }
    public class GraphExporter {
        public static void exportPanelAsPNG(JPanel panel, String filename) {
            BufferedImage image = new BufferedImage(
                    panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
            panel.paint(image.getGraphics());
            try {
                ImageIO.write(image, "png", new File(filename));
                System.out.println("✅ Graph exporté : " + filename);
            } catch (Exception e) {
                System.err.println("❌ Échec de l'export PNG : " + e.getMessage());
            }
        }
    }
}
