
/*
 * DungGraphPanel.java
 * Panneau Swing pour visualiser le graphe d'argumentation avec légende.
 */
package org.example.ArgumentationDM;

//tweety
import org.tweetyproject.arg.dung.syntax.Argument;
import org.tweetyproject.arg.dung.syntax.DungTheory;

//swing
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

    // Mapping argument => mode pour la couleur
    private static final Map<String,String> MODE_MAP = Map.ofEntries(
            Map.entry("A13","CAR"), Map.entry("A16","CAR"), Map.entry("A17","CAR"), Map.entry("A18","CAR"),
            Map.entry("A3","PUBLIC_TRANSPORT"), Map.entry("A10","PUBLIC_TRANSPORT"),
            Map.entry("A22","PUBLIC_TRANSPORT"), Map.entry("A25","PUBLIC_TRANSPORT"),
            Map.entry("A5","WALK"), Map.entry("A11","WALK"), Map.entry("A14","WALK"), Map.entry("A19","WALK"),
            Map.entry("A7","BIKE"), Map.entry("A12","BIKE"), Map.entry("A21","BIKE"), Map.entry("A23","BIKE"),
            Map.entry("A33", "WALK"),  // Santé faible, marche difficile
            Map.entry("A41", "PUBLIC_TRANSPORT"),
            Map.entry("A37", "BIKE"),Map.entry("A35", "Santé vélo-"),
            Map.entry("A36", "Danger pluie vélo"),
            Map.entry("A50", "Long trajet marche")
            );

    // Labels lisibles pour chaque argument
    private static final Map<String,String> LABELS = Map.ofEntries(
            Map.entry("A13","Confort"), Map.entry("A18","Rapide"), Map.entry("A28","Disponible"),
            Map.entry("A30","Familles"), Map.entry("A60","Sec"), Map.entry("A61","Securite"),
            Map.entry("A62","Charge"), Map.entry("A63","Long trajet"), Map.entry("A64","Climatisation"),
            Map.entry("A3","Pas conduite"), Map.entry("A10","Travail/lecture"),
            Map.entry("A22","Pas parking"), Map.entry("A25","Réseau dense"),
            Map.entry("A5","Gratuit"), Map.entry("A11","Courte dist."),
            Map.entry("A14","Relaxant"), Map.entry("A19","Sain"),
            Map.entry("A7","Rapide urbain"), Map.entry("A12","Evite trafic"),
            Map.entry("A21","Ecologique"), Map.entry("A23","Pistes vélo"),
            Map.entry("A1","Cher"), Map.entry("A2","Coût justifié"),
            Map.entry("A4","Attente"), Map.entry("A6","Lent"),
            Map.entry("A8","Fatigant"), Map.entry("A9","Bouchons"),
            Map.entry("A24","Pollution"), Map.entry("A26","Surcharge"),
            Map.entry("A31","Lignes rares"), Map.entry("A40","Inconfort"),
            Map.entry("A33", "Fatigue santé"),
            Map.entry("A41", "Réseau saturé"),Map.entry("A37", "BIKE"),
            Map.entry("A35", "BIKE"),
            Map.entry("A36", "BIKE"),
            Map.entry("A50", "WALK")
    );

    public DungGraphPanel(DungTheory theory, Collection<Argument> accepted) {
        this.theory = theory;
        this.accepted = new HashSet<>(accepted);
        setPreferredSize(new Dimension(1000,800));
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int radius = 40;
        int cx = getWidth()/2, cy = getHeight()/2;
        int cr = Math.min(cx, cy) - 150;
        List<Argument> args = new ArrayList<>(theory);
        Map<Argument, Point> pos = new HashMap<>();
        for (int i=0; i<args.size(); i++) {
            double ang = 2*Math.PI*i/args.size();
            int x = cx + (int)(cr*Math.cos(ang));
            int y = cy + (int)(cr*Math.sin(ang));
            pos.put(args.get(i), new Point(x,y));
        }
        //attaques
        g2.setStroke(new BasicStroke(2));
        for (Argument a: args) for (Argument b: theory.getAttacked(a)) {
            drawArrow(g2, pos.get(a), pos.get(b));
        }
        //noeuds
        for (Argument a: args) {
            Point p = pos.get(a);
            String nm = a.getName();
            boolean acc = accepted.contains(a);
            String md = MODE_MAP.getOrDefault(nm,"OTHER");
            Color col = switch(md) {
                case "CAR" -> new Color(66,133,244);
                case "PUBLIC_TRANSPORT" -> new Color(156,39,176);
                case "WALK" -> new Color(255,215,0);
                case "BIKE" -> new Color(255,111,0);
                default -> Color.LIGHT_GRAY;
            };
            g2.setColor(col);
            if (acc) g2.fillOval(p.x-radius/2,p.y-radius/2,radius,radius);
            g2.setColor(Color.BLACK);
            g2.drawOval(p.x-radius/2,p.y-radius/2,radius,radius);
            String label = LABELS.getOrDefault(nm,nm);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, p.x - fm.stringWidth(label)/2, p.y + radius);
        }
        drawLegend(g2);
    }
    private void drawArrow(Graphics2D g2, Point from, Point to) {
        double dx=to.x-from.x, dy=to.y-from.y;
        double ang=Math.atan2(dy,dx);
        AffineTransform old = g2.getTransform();
        g2.translate(from.x,from.y);
        g2.rotate(ang);
        int len=(int)Math.hypot(dx,dy);
        g2.drawLine(0,0,len-10,0);
        Polygon head=new Polygon(new int[]{len-10,len-16,len-16}, new int[]{0,-6,6},3);
        g2.fill(head);
        g2.setTransform(old);
    }
    private void drawLegend(Graphics2D g2) {
        int x=getWidth()-200, y=getHeight()-180;
        g2.setColor(Color.BLACK);
        g2.drawString("● = accepté    ○ = rejeté", x,y);
        y+=20;
        Map<String,Color> lc=Map.of(
                "CAR",new Color(66,133,244),
                "PT",new Color(156,39,176),
                "WALK",new Color(255,215,0),
                "BIKE",new Color(255,111,0)
        );
        for (var e: lc.entrySet()) {
            g2.setColor(e.getValue());
            g2.fillRect(x,y,12,12);
            g2.setColor(Color.BLACK);
            g2.drawString(e.getKey(), x+20, y+12);
            y+=20;
        }
    }
     //Exporte en PNG
    public static class GraphExporter {
        public static void exportPanelAsPNG(JPanel panel, String filename) {
            BufferedImage img = new BufferedImage(
                    panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
            panel.paint(img.getGraphics());
            try { ImageIO.write(img,"png",new File(filename));
                System.out.println("✅ Exporté : " + filename);
            } catch(Exception e){ System.err.println("❌ Erreur export : "+e.getMessage()); }
        }
    }
}
