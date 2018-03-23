package graph.summary;

import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.*;
import splitstrategies.SplitStrategy;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Collectors;

import static guru.nidi.graphviz.model.Factory.*;

/**
 * Created by lukas on 12.03.18.
 */
public class Summary extends BaseGraph {

    private int LARGE_MEMORY_NUMBER = 160000000;

    private BaseGraph baseGraph;
    private SplitStrategy splitStrategy;

    public static Summary createFromGraph(BaseGraph baseGraph, SplitStrategy splitStrategy){
        Summary s = new Summary(baseGraph, splitStrategy);
        List<String> nodeLabels = baseGraph.getNodes().stream().map(BaseNode::getLabel).collect(Collectors.toList());
        Set<String> edgeLabels = baseGraph.getEdges().stream().map(BaseEdge::getLabel).collect(Collectors.toSet());
        SummaryNode node = new SummaryNode(0, new HashSet<>(nodeLabels));
        s.addNode(node);
        for (String edgeLabel: edgeLabels){
            s.addSEdge(node, node, edgeLabel);
        }
        return s;
    }

    public Summary(BaseGraph baseGraph, SplitStrategy splitStrategy) {
        super();
        this.baseGraph = baseGraph;
        this.splitStrategy = splitStrategy;
    }

    @Override
    public List<List<String>> query(BaseGraph query){
        List<List<String>> raw = super.query(query);
        List<List<String>> result = new ArrayList<>();
        List<String> variables = query.getVariables();
        for (List<String> entry: raw){
            List<List<String>> unfolded = entry.stream()
                    .map(s -> Arrays.asList(s.split("#"))).collect(Collectors.toList());
            int cartesianCount = unfolded.stream().map(List::size).reduce(1, (a, b) -> a * b);
            String[][] unfoldedResults = new String[cartesianCount][variables.size()];
            int numberIterations = 1;
            for (int i = 0, c = 0; i < variables.size(); i++, c = 0){
                int blockSize = cartesianCount / unfolded.get(i).size() / numberIterations;
                for (int j = 0; j < numberIterations; j++){
                    for (String answer: unfolded.get(i)){
                        for (int k = 0; k < blockSize; k++){
                            unfoldedResults[c++][i] = answer;
                        }
                    }
                }
                numberIterations = numberIterations * unfolded.get(i).size();
            }
            for (String[] array : unfoldedResults) {
                result.add(Arrays.asList(array));
            }
        }
        return result;
    }

    public double measure(BaseGraph query){
        int truePositives = 0, falsePositives = 0, falseNegatives = 0;
        List<List<String>> summaryResults = query(query);
        List<List<String>> graphResults = getBaseGraph().query(query);
        for (List<String> result: summaryResults){
            if (graphResults.contains(result)){
                truePositives++;
            } else{
                falsePositives++;
            }
        }
        for (List<String> result: graphResults){
            if (!summaryResults.contains(result)){
                falseNegatives++;
            }
        }
        double precision = truePositives * 1.0 / (truePositives + falsePositives);
        double recall = truePositives * 1.0 / (truePositives + falseNegatives);
        return 2 * precision * recall / (precision + recall);
    }

    public void split(){
        this.splitStrategy.split(this);
    }

    public void addSEdge(SummaryNode source, SummaryNode target, String label) {
        SummaryEdge edge = new SummaryEdge(source, target, label);
        edge.setActual(supportOf(edge));
        getEdges().add(edge);
        getOutIndex().get(source).add(edge);
        getInIndex().get(target).add(edge);
    }

    public long supportOf(SummaryEdge edge){
        return this.baseGraph.getEdges().stream().filter(e -> e.getLabel().equals(edge.getLabel())
                && edge.getSSource().getLabels().contains(e.getSource().getLabel())
                && edge.getSTarget().getLabels().contains(e.getTarget().getLabel())).count();
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
            edgeMapping.get(source).add(to(target).with(Label.of(edge.getLabel())));
        }
        Graph g = graph("ex").directed().with(mapping.values().stream().map(node ->
                node.link(edgeMapping.get(node).toArray(new LinkTarget[edgeMapping.get(node).size()]))).toArray(Node[]::new));
        Graphviz viz = Graphviz.fromGraph(g).totalMemory(LARGE_MEMORY_NUMBER);
        BufferedImage image = viz.render(Format.PNG).toImage();
        JDialog dialog = new JDialog();
        dialog.getContentPane().add(new JLabel(new ImageIcon(image)));
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setVisible(true);
    }

    public BaseGraph getBaseGraph(){
        return baseGraph;
    }

}
