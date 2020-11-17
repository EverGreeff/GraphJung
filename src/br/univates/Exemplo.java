package br.univates;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants; 

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.generators.random.EppsteinPowerLawGenerator;
import edu.uci.ics.jung.algorithms.layout.*;
import edu.uci.ics.jung.algorithms.shortestpath.BFSDistanceLabeler;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer;

/* Demonstracao do calculo e exibicao do algoritmo de caminho minimo
 */
public class Exemplo extends JPanel {

    private String origem;
    private String destino;
    private Graph<String, Number> grafo;
    private Set<String> caminho;

    public Exemplo() {
        // chama metodo auxiliar que constroi o grafo
        this.grafo = criaGrafo();

        // define layout do grafo
        final Layout<String, Number> layout = new FRLayout<String, Number>(grafo);
        //final Layout<String,Number> layout = new KKLayout<String,Number>(grafo);
        final VisualizationViewer<String, Number> vv = new VisualizationViewer<String, Number>(layout);
        vv.setBackground(Color.WHITE);

        // define como desenhar os vertices e exibir suas legendas
        vv.getRenderContext().setVertexDrawPaintTransformer(new PintarVertice<String>());
        vv.getRenderContext().setVertexFillPaintTransformer(new PreencherVertice<String>());
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<String>());

        // define como desenhar as arestas e exibir suas legendas
        vv.getRenderContext().setEdgeDrawPaintTransformer(new PintarAresta());
        //vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<Number>());

        vv.setGraphMouse(new DefaultModalGraphMouse<String, Number>());
        vv.addPostRenderPaintable(new VisualizationViewer.Paintable() {
            public boolean useTransform() {
                return true;
            }

            public void paint(Graphics g) {
                if (caminho != null) {
                    // para cada aresta, pinte as que fazem parte do caminho minimo
                    for (Number e : layout.getGraph().getEdges()) {
                        if (marcado(e)) {
                            String v1 = grafo.getEndpoints(e).getFirst();
                            String v2 = grafo.getEndpoints(e).getSecond();
                            Point2D p1 = layout.transform(v1);
                            Point2D p2 = layout.transform(v2);
                            p1 = vv.getRenderContext().getMultiLayerTransformer().transform(Layer.LAYOUT, p1);
                            p2 = vv.getRenderContext().getMultiLayerTransformer().transform(Layer.LAYOUT, p2);
                            Renderer<String, Number> renderer = vv.getRenderer();
                            renderer.renderEdge(vv.getRenderContext(), layout, e);
                        }
                    }
                }
            }
        });
        setLayout(new BorderLayout());
        add(vv, BorderLayout.CENTER);
        // define controles
        add(controles(), BorderLayout.SOUTH);
    }

    boolean marcado(Number e) {
        Pair<String> aresta = grafo.getEndpoints(e);
        String v1 = aresta.getFirst();
        String v2 = aresta.getSecond();
        return v1.equals(v2) == false && caminho.contains(v1) && caminho.contains(v2);
    }

    public class PintarVertice<V> implements Transformer<V, Paint> {

        public Paint transform(V v) {
            return Color.black;
        }
    }

    public class PreencherVertice<V> implements Transformer<V, Paint> {

        public Paint transform(V v) {
            if (v == origem) {
                return Color.BLUE;
            }
            if (v == destino) {
                return Color.BLUE;
            }
            if (caminho == null) {
                return Color.LIGHT_GRAY;
            } else {
                if (caminho.contains(v)) {
                    return Color.RED;
                } else {
                    return Color.LIGHT_GRAY;
                }
            }
        }
    }

    public class PintarAresta implements Transformer<Number, Paint> {

        public Paint transform(Number e) {
            if (caminho == null || caminho.size() == 0) {
                return Color.BLACK;
            }
            if (marcado(e)) {
                return new Color(0.0f, 0.0f, 1.0f, 0.5f);
            } else {
                return Color.LIGHT_GRAY;
            }
        }
    }

    private JPanel controles() {
        JPanel jp = new JPanel();
        jp.setLayout(new BoxLayout(jp, BoxLayout.PAGE_AXIS));
        jp.setBorder(BorderFactory.createLineBorder(Color.black, 3));
        jp.add(new JLabel("Selecione um par de vertices!"));
        JPanel jp2 = new JPanel();
        jp2.add(new JLabel("origem:", SwingConstants.LEFT));
        jp2.add(selecionar(true));
        JPanel jp3 = new JPanel();
        jp3.add(new JLabel("destino:", SwingConstants.LEFT));
        jp3.add(selecionar(false));
        jp.add(jp2);
        jp.add(jp3);
        return jp;
    }

    private Component selecionar(final boolean inicio) {
        Set<String> s = new TreeSet<String>();
        for (String v : grafo.getVertices()) {
            s.add(v);
        }
        final JComboBox opcoes = new JComboBox(s.toArray());
        opcoes.setSelectedIndex(-1);
        opcoes.setBackground(Color.WHITE);
        opcoes.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String v = (String) opcoes.getSelectedItem();
                if (inicio) {
                    origem = v;
                } else {
                    destino = v;
                }
                calculaCaminho();
                repaint();
            }
        });
        return opcoes;
    }

    protected void calculaCaminho() {
        if (origem == null || destino == null) {
            return;
        }
        BFSDistanceLabeler<String, Number> bdl = new BFSDistanceLabeler<String, Number>();
        bdl.labelDistances(grafo, origem);
        caminho = new HashSet<String>();

        // obtem caminho andando para tras, a partir do destino
        String v = destino;
        Set<String> pred = bdl.getPredecessors(v);
        caminho.add(destino);
        while (pred != null && pred.size() > 0) {
            v = pred.iterator().next();
            caminho.add(v);
            if (v == origem) {
                return;
            }
            pred = bdl.getPredecessors(v);
        }
    }

    public static void main(String[] args) {
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.getContentPane().add(new Exemplo());
        jf.pack();
        jf.setVisible(true);
    }

    /*---------------- Constroi e retorna o grafo utilizado ----------------*/
    private Graph<String, Number> criaGrafo() {
        // cria grafo aleatorio
        Graph<String, Number> g = new EppsteinPowerLawGenerator<String, Number>(new GraphFactory(), new VertexFactory(), new EdgeFactory(), 26, 50, 50).create();

        // elimina eventuais vertices sem arestas
        Set<String> remover = new HashSet<String>();
        for (String v : g.getVertices()) {
            if (g.degree(v) == 0) {
                remover.add(v);
            }
        }
        for (String v : remover) {
            g.removeVertex(v);
        }

        return g;
    }

    static class GraphFactory implements Factory<Graph<String, Number>> {

        public Graph<String, Number> create() {
            return new SparseGraph<String, Number>();
        }
    }

    static class VertexFactory implements Factory<String> {

        char c = 'A';

        public String create() {
            return Character.toString(c++);
        }
    }

    static class EdgeFactory implements Factory<Number> {

        int count;

        public Number create() {
            return count++;
        }
    }
}
