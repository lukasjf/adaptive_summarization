package graph;

import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.node;
import static guru.nidi.graphviz.model.Factory.to;

/**
 * Created by lukas on 12.03.18.
 */
public class BaseGraph implements GraphQueryAble{

    //for drawing
    private int LARGE_MEMORY_NUMBER = 160000000;
    private int imagecount = 1;

    //text to append to a label to make it unique -- realised by an increasing number
    private static int DEDUPLICATE_COUNTER = 0;

    private boolean original;

    Set<BaseNode> nodes = new HashSet<>(30000);
    HashMap<Integer, BaseNode> idMapping = new HashMap<>(30000);
    HashMap<String, BaseNode> labelMapping = new HashMap<>(30000);

    Set<BaseEdge> edges= new HashSet<>(50000);
    HashMap<Integer, List<BaseEdge>> inIndex = new HashMap<>(30000);
    HashMap<Integer, List<BaseEdge>> outIndex = new HashMap<>(30000);

    public BaseGraph(){
        original = false;
    }

    public BaseGraph(boolean original){
        this.original = original;
    }


    public BaseNode addNode(int id, String label){
        if (idMapping.keySet().contains(id)){
            System.err.println("ID already in use: " + id);
            return null;
        }
        String newlabel = label;
        while (labelMapping.keySet().contains(newlabel)){
            newlabel += DEDUPLICATE_COUNTER++;
        }
        BaseNode node = new BaseNode(id);
        nodes.add(node);

        idMapping.put(id, node);
        labelMapping.put(newlabel, node);

        if (original){
            M.addPair(newlabel, id);
        }

        inIndex.put(id, new ArrayList<>());
        outIndex.put(id, new ArrayList<>());

        return node;
    }

    public void removeNode(int id){
        String label = M.labelFrom(id);
        BaseNode node = idMapping.get(id);

        nodes.remove(node);

        idMapping.remove(id);
        labelMapping.remove(label);

        M.remove(id);

        for (BaseEdge e: inIndex.get(id)){
            edges.remove(e);
            outIndex.get(e.getSource().getId()).remove(e);
        }
        for (BaseEdge e: outIndex.get(id)){
            edges.remove(e);
            inIndex.get(e.getTarget().getId()).remove(e);
        }
        inIndex.remove(id);
        outIndex.remove(id);
    }

    public BaseEdge addEdge(int source, int target, String label){
        if (outIndex.get(source).stream().anyMatch(e -> e.getTarget().getId() == target & e.getLabel().equals(label))){
            System.err.println("Trying to insert duplicate edge: " + source + " " + target + " " + label);
            return null;
        }
        BaseEdge e = new BaseEdge(idMapping.get(source), idMapping.get(target), label);
        edges.add(e);
        inIndex.get(target).add(e);
        outIndex.get(source).add(e);
        return e;
    }

    public void removeEdge(BaseEdge edge){
        edges.remove(edge);
        outIndex.get(edge.getSource().getId()).remove(edge);
        inIndex.get(edge.getTarget().getId()).remove(edge);
    }


    @Override
    public List<Map<String, String>> query(BaseGraph query) {
        return new SubgraphIsomorphism().query(query, this, true);
    }

    public int getNumberEdgeTypes(){
        return edges.stream().map(BaseEdge::getLabel).collect(Collectors.toSet()).size();
    }

    public BaseNode nodeWithId(int id){
        return idMapping.get(id);
    }

    public List<BaseEdge> outEdgesFor(int id){
        return outIndex.get(id);
    }

    public List<BaseEdge> inEdgesFor(int id){
        return inIndex.get(id);
    }

    public Set<BaseNode> getNodes() {
        return nodes;
    }

    public HashMap<Integer, BaseNode> getIdMapping() {
        return idMapping;
    }

    public HashMap<String, BaseNode> getLabelMapping() {
        return labelMapping;
    }

    public Set<BaseEdge> getEdges() {
        return edges;
    }

    public HashMap<Integer, List<BaseEdge>> getInIndex() {
        return inIndex;
    }

    public HashMap<Integer, List<BaseEdge>> getOutIndex() {
        return outIndex;
    }

    public void draw(){
        Map<BaseNode, Node> mapping = new HashMap<>();
        Map<Node, List<Link>> edgeMapping = new HashMap<>();
        for (BaseNode node: getNodes()){
            Node drawingNode = node(node.toString());
            mapping.put(node, drawingNode);
            edgeMapping.put(drawingNode, new ArrayList<>());
        }
        for (BaseEdge edge: getEdges()){
            Node source = mapping.get(edge.getSource());
            Node target = mapping.get(edge.getTarget());
            edgeMapping.get(source).add(to(target).with(Label.of(edge.toString())));
        }
        Graph g = graph("ex").directed().with(mapping.values().stream().map(node ->
                node.link(edgeMapping.get(node).toArray(new LinkTarget[edgeMapping.get(node).size()]))).toArray(Node[]::new));
        Graphviz viz = Graphviz.fromGraph(g).totalMemory(LARGE_MEMORY_NUMBER);
        BufferedImage image = viz.engine(Engine.NEATO).render(Format.SVG).toImage();
        File img = new File("test" + imagecount++ + ".png");
        try {
            ImageIO.write(image, "png", img);
        } catch (IOException e) {
            e.printStackTrace();
        }
        JDialog dialog = new JDialog();
        dialog.getContentPane().add(new JLabel(new ImageIcon(image)));
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setVisible(true);
    }

}
