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
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import java.awt.Button;
import java.awt.List;
import java.util.ArrayList;

/* Demonstracao do calculo e exibicao do algoritmo de caminho minimo
 */
public class Exemplo extends JPanel {

    private String origem;
    private String destino;
    private Graph<String, Double> grafo;
    private Set<String> caminho;
    private String maior;
    private String menor;
    private int nroCitysMidle;

    public Exemplo() {
        // chama metodo auxiliar que constroi o grafo
        this.grafo = criaGrafo();

        // define layout do grafo
        final Layout<String, Double> layout = new FRLayout<String, Double>(grafo);
        //final Layout<String,Double> layout = new KKLayout<String,Double>(grafo);
        final VisualizationViewer<String, Double> vv = new VisualizationViewer<String, Double>(layout);
        vv.setBackground(Color.WHITE);

        // define como desenhar os vertices e exibir suas legendas
        vv.getRenderContext().setVertexDrawPaintTransformer(new PintarVertice<String>());
        vv.getRenderContext().setVertexFillPaintTransformer(new PreencherVertice<String>());
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<String>());

        // define como desenhar as arestas e exibir suas legendas
        vv.getRenderContext().setEdgeDrawPaintTransformer(new PintarAresta());
        //vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<Double>());

        vv.setGraphMouse(new DefaultModalGraphMouse<String, Double>());
        vv.addPostRenderPaintable(new VisualizationViewer.Paintable() {
            public boolean useTransform() {
                return true;
            }

            public void paint(Graphics g) {
                if (caminho != null) {
                    // para cada aresta, pinte as que fazem parte do caminho minimo
                    for (Double e : layout.getGraph().getEdges()) {
                        if (marcado(e)) {
                            String v1 = grafo.getEndpoints(e).getFirst();
                            String v2 = grafo.getEndpoints(e).getSecond();
                            Point2D p1 = layout.transform(v1);
                            Point2D p2 = layout.transform(v2);
                            p1 = vv.getRenderContext().getMultiLayerTransformer().transform(Layer.LAYOUT, p1);
                            p2 = vv.getRenderContext().getMultiLayerTransformer().transform(Layer.LAYOUT, p2);
                            Renderer<String, Double> renderer = vv.getRenderer();
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

    boolean marcado(Double e) {
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
            if (v == maior) {
                return Color.RED;
            }
            if (v == menor) {
                return Color.BLUE;
            }
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

    public class PintarAresta implements Transformer<Double, Paint> {

        public Paint transform(Double e) {
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
        JPanel jp4 = new JPanel();
        Button btn1 = new Button("Par de cidades com maior distância entre si");
        btn1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                zera();
                MaiorDistancia();
                repaint();
            }
        });
        jp4.add(btn1);
        JPanel jp5 = new JPanel();
        Button btn2 = new Button("Par de cidades com o maior número de cidades intermediárias entre si");
        btn2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                zera();
                MaiorNroCidades();
                repaint();
            }
        });
        jp5.add(btn2);
        JPanel jp6 = new JPanel();
        Button btn3 = new Button("Maior e menor conectividade");
        btn3.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                zera();
                MaiorMenorConectividade();
                repaint();
            }
        });
        jp6.add(btn3);
        jp.add(jp2);
        jp.add(jp3);
        jp.add(jp4);
        jp.add(jp5);
        jp.add(jp6);
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
        BFSDistanceLabeler<String, Double> bdl = new BFSDistanceLabeler<String, Double>();
        bdl.labelDistances(grafo, origem);
        caminho = new HashSet<String>();

        // obtem caminho andando para tras, a partir do destino
        String v = destino;
        Set<String> pred = bdl.getPredecessors(v);
        caminho.add(destino);
        nroCitysMidle = 0;
        while (pred != null && pred.size() > 0) {
            v = pred.iterator().next();
            caminho.add(v);
            if (v == origem) {
                return;
            } 
            if (v != destino) {
                nroCitysMidle += 1;
            }
            pred = bdl.getPredecessors(v);
        }
        return;
    }

    public static void main(String[] args) {
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.getContentPane().add(new Exemplo());
        jf.pack();
        jf.setVisible(true);
    }

    /*---------------- Constroi e retorna o grafo utilizado ----------------*/
    private Graph<String, Double> criaGrafo() {

        Graph<String, Double> g = new SparseMultigraph<String, Double>();
        g.addEdge(9.8, "Westfália", "Teutônia");
        g.addEdge(19.7, "Imigrante", "Teutônia");
        g.addEdge(11.6, "Westfália", "Imigrante");
        g.addEdge(25.0, "Estrêla", "Westfália");
        g.addEdge(21.5, "Estrêla", "Teutônia");
        g.addEdge(23.5, "Lajeado", "Teutônia");
        g.addEdge(4.6, "Lajeado", "Estrêla");
        g.addEdge(26.9, "Lajeado", "Westfália");
        g.addEdge(35.4, "Imigrante", "Garibaldi");
        g.addEdge(48.0, "Imigrante", "Bento Gonçalves");
        g.addEdge(75.4, "Lajeado", "Bento Gonçalves");
        g.addEdge(113.0, "Lajeado", "Porto Alegre");
        g.addEdge(50.4, "Portão", "Porto Alegre");
        g.addEdge(86.8, "Portão", "Lajeado");
        g.addEdge(22.7, "Portão", "Montenegro");
        g.addEdge(64.7, "Lajeado", "Montenegro");
        g.addEdge(79.4, "Imigrante", "Montenegro");
        g.addEdge(78.4, "Imigrante", "Caxias do Sul");
        g.addEdge(69.2, "Gramado", "Caxias do Sul");
        g.addEdge(81.1, "Farroupilha", "Lajeado");
        g.addEdge(22.6, "Lajeado", "Fazenda Vilanova");
        g.addEdge(31.5, "Fazenda Vilanova", "Taquari");
        g.addEdge(58.2, "Westfália", "Taquari");
        g.addEdge(81.6, "Santa Cruz do Sul", "Taquari");


        return g;
    }

    static class GraphFactory implements Factory<Graph<String, Double>> {

        public Graph<String, Double> create() {
            return new SparseGraph<String, Double>();
        }
    }

    static class VertexFactory implements Factory<String> {

        char c = 'A';

        public String create() {
            return Character.toString(c++);
        }
    }

    static class EdgeFactory implements Factory<Double> {

        double count;

        public Double create() {
            return count++;
        }
    }
    
    private ArrayList<String> getCitys() {
        ArrayList<String> citys = new ArrayList();
        
        for (Double edge : grafo.getEdges()) {
            for (String incidentVertice : grafo.getIncidentVertices(edge)) {
                if (!citys.contains(incidentVertice)) {
                    citys.add(incidentVertice);
                }
            }
        }
        
        return citys;
    }
    
    private void zera() {
        maior = "";
        menor = "";
        origem = "";
        destino = "";
        caminho = null;
        nroCitysMidle = -1;
    }
    
    private void MaiorDistancia() {
        String resultOrigem = "";
        String resultDestino = "";
        double resultDistancia = 0.0;
        for (String cityOrigem : getCitys()) {
            for (String cityDestino : getCitys()) {
                this.origem = cityOrigem;
                this.destino = cityDestino;
                calculaCaminho();
//                if (countCitys > nro) {
//                    nro = countCitys;
                    resultOrigem = cityOrigem;
                    resultDestino = cityDestino;
//                }
            }
        }
        this.origem = resultOrigem;
        this.destino = resultDestino;
        calculaCaminho();
    }
    
    private void MaiorNroCidades() {
        String resultOrigem = "";
        String resultDestino = "";
        int nro = 0;
        for (String cityOrigem : getCitys()) {
            for (String cityDestino : getCitys()) {
                this.origem = cityOrigem;
                this.destino = cityDestino;
                calculaCaminho();
                if (nroCitysMidle > nro) {
                    nro = nroCitysMidle;
                    resultOrigem = cityOrigem;
                    resultDestino = cityDestino;
                }
            }
        }
        this.origem = resultOrigem;
        this.destino = resultDestino;
        calculaCaminho();
    }
    
    private void MaiorMenorConectividade() {
        int menorC = 99;
        String nameMenorC = "";
        int maiorC = 0;
        String nameMaiorC = "";
        
        for (String city : getCitys()) {
            int conects = grafo.getInEdges(city).size();
            if (conects > maiorC) {
                maiorC = conects;
                nameMaiorC = city;
            }
            if (conects < menorC) {
                menorC = conects;
                nameMenorC = city;
            }
        }
        
        PintarAresta aresta = new PintarAresta();
        
        maior = nameMaiorC;
        menor = nameMenorC;
    }
}
