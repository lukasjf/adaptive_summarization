package graph.summary;

import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.*;
import splitstrategies.SplitStrategy;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static guru.nidi.graphviz.model.Factory.*;

/**
 * Created by lukas on 12.03.18.
 */
public class Summary extends BaseGraph {

    private int LARGE_MEMORY_NUMBER = 160000000;
    private int imagecount = 1;

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
    public List<String[]> query(BaseGraph query){
        List<Map<BaseEdge, SummaryEdge>> matchings = new SummaryIsomorphism(this).query(query);
        List<String[]> results = new ArrayList<>();

        for (Map<BaseEdge, SummaryEdge> match: matchings){
            Map<BaseNode, SummaryNode> nodeMatch = new HashMap<>();
            Set<BaseNode> doneVariables=  new HashSet<>();

            for (BaseEdge queryEdge: match.keySet()){
                if (queryEdge.getSource().isVariable() && ! doneVariables.contains(queryEdge.getSource())){
                    doneVariables.add(queryEdge.getSource());
                    nodeMatch.put(queryEdge.getSource(), match.get(queryEdge).getSSource());
                }
                if (queryEdge.getTarget().isVariable() && ! doneVariables.contains(queryEdge.getTarget())){
                    doneVariables.add(queryEdge.getTarget());
                    nodeMatch.put(queryEdge.getTarget(), match.get(queryEdge).getSTarget());
                }
            }

            List<String> variables = query.getVariables();
            int cartesianCount = (int) getSizeOfMatch(match);
            results.addAll(getCrossProductForMatch(nodeMatch, variables, cartesianCount));
        }
        return results;
    }

    private List<String[]> getCrossProductForMatch(Map<BaseNode, SummaryNode> nodeMatch, List<String> variables, int matchSize){
        List<String[]> matchResult = new ArrayList<>();
        String[][] unfoldedResults = new String[matchSize][variables.size()];
        int numberIterations = 1;
        for (BaseNode queryNode: nodeMatch.keySet()){
            int nodeIndex = variables.indexOf(queryNode.getLabel());
            int rowCounter = 0;
            int blockSize = matchSize / nodeMatch.get(queryNode).size() / numberIterations;
            for (int j = 0; j < numberIterations; j++){
                for (String answer: nodeMatch.get(queryNode).getLabels()){
                    for (int k = 0; k < blockSize; k++){
                        unfoldedResults[rowCounter++][nodeIndex] = answer;
                    }
                }
            }
        }
        matchResult.addAll(Arrays.asList(unfoldedResults));
        return matchResult;
    }

    private long getSizeOfMatch(Map<BaseEdge, SummaryEdge> match){
        long matchSize = 1L;
        Set<BaseNode> doneVariables = new HashSet<>();
        for (BaseEdge queryEdge: match.keySet()){
            if (queryEdge.getSource().isVariable() && ! doneVariables.contains(queryEdge.getSource())){
                doneVariables.add(queryEdge.getSource());
                matchSize *= match.get(queryEdge).getSSource().size();
            }
            if (queryEdge.getTarget().isVariable() && ! doneVariables.contains(queryEdge.getTarget())){
                doneVariables.add(queryEdge.getTarget());
                matchSize *= match.get(queryEdge).getSTarget().size();
            }
        }
        return matchSize;
    }


    private void updateBookKeeping(List<Map<BaseEdge, SummaryEdge>> matchings, BaseGraph query, long results) {
        for (Map<BaseEdge, SummaryEdge> match: matchings){
            for (BaseEdge queryEdge: match.keySet()){
                if (queryEdge.getSource().isVariable() || queryEdge.getTarget().isVariable()){
                    addQueryLoss(match.get(queryEdge), query, results);
                }
            }
        }
    }

    private void addQueryLoss(SummaryEdge sEdge, BaseGraph query, long results){
        double loss = (double) sEdge.bookKeeping.getOrDefault("queryLoss", 0.0);
        loss += Math.log(1.0 / sEdge.getSupport());
        sEdge.bookKeeping.put("queryLoss", loss);
    }

    public long getResultSize(BaseGraph query) {
        List<Map<BaseEdge, SummaryEdge>> matchings = new SummaryIsomorphism(this).query(query);
        long results = 0L;
        for (Map<BaseEdge, SummaryEdge> match: matchings){
            long matchSize = getSizeOfMatch(match);
            results += matchSize;
        }
        updateBookKeeping(matchings, query, results);
        return results;
    }

    public void split(){
        this.splitStrategy.split(this);
    }

    public void addSEdge(SummaryNode source, SummaryNode target, String label) {
        SummaryEdge edge = new SummaryEdge(source, target, label);
        long actual = supportOf(edge);
        if (actual > 0){
            edge.setActual(actual);
            getEdges().add(edge);
            getOutIndex().get(source).add(edge);
            getInIndex().get(target).add(edge);
        }
    }

    public long supportOf(SummaryEdge edge){
        long result = 0;
        for (String label: edge.getSSource().getLabels()){
            List<BaseEdge> edges = getBaseGraph().getOutIndex().get(getBaseGraph().getLabelMapping().get(label));
            for (BaseEdge e: edges){
                if (e.getLabel().equals(edge.getLabel()) && edge.getSTarget().getLabels().contains(e.getTarget().getLabel())){
                    result ++;
                }
            }
        }
        return result;
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
        BufferedImage image = viz.render(Format.PNG).toImage();
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

    public BaseGraph getBaseGraph(){
        return baseGraph;
    }

}
