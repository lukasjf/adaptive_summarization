package main;

import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;
import graph.GraphImporter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

/**
 * Created by lukas on 19.03.18.
 */
public class QueryGenerator {

    private static int LOWER_SIZE = 2;
    private static int UPPER_SIZE = 3;
    private static int QUERIES_PER_SIZE = 25;
    private static int MAX_TRIES = 10000;

    private static int TEST_QUERIES_PER_SIZE = 10;

    private BaseGraph graph;
    private Random random = new Random(42);

    private int queryCounter = 1;
    private boolean isTest;

    private String outputDir;

    private List<BaseNode> seedNodes = new ArrayList<>();

    public QueryGenerator(String input, String output, List<String> seeds){
        this.outputDir = output;
        File outDir = new File(outputDir);
        if (!outDir.exists()){
            outDir.mkdir();
            new File(outputDir + "train").mkdir();
            new File(outputDir + "test").mkdir();
        }
        this.graph = GraphImporter.parseGraph(input);

        for (String seed: seeds){
            BaseNode seedNode = graph.getLabelMapping().get(seed);
            addSeedNode(seedNode);
            for (BaseEdge edge: graph.inEdgesFor(seedNode.getId())){
                addSeedNode(edge.getSource());
            }
            for (BaseEdge edge: graph.outEdgesFor(seedNode.getId())) {
                addSeedNode(edge.getTarget());
            }
        }
        if (seeds.isEmpty()){
            seedNodes.addAll(graph.getNodes());
        }
    }

    public void addSeedNode(BaseNode node){
        if (! seedNodes.contains(node)){
            seedNodes.add(node);
        }
    }

    public void generate(boolean isTest){
        this.isTest = isTest;
        for (int s = LOWER_SIZE; s <= UPPER_SIZE; s++) {
            int sizeBound = isTest ? TEST_QUERIES_PER_SIZE : QUERIES_PER_SIZE;
            for (int i = 0; i < sizeBound; i++) {
                List<BaseEdge> candidates = new ArrayList<>();
                List<BaseEdge> queryGraph = new ArrayList<>();

                int startIndex = random.nextInt(seedNodes.size());
                BaseNode startNode = seedNodes.get(startIndex);
                candidates.addAll(graph.inEdgesFor(startNode.getId()));
                candidates.addAll(graph.outEdgesFor(startNode.getId()));
                if (candidates.isEmpty()){
                    i--;
                    System.out.println("#");
                    continue;
                }

                for (int k = 1; k < s; k++){
                    BaseEdge taken = null;
                    int tries = 0;
                    boolean searching = true;
                    while (!candidates.isEmpty() && tries < MAX_TRIES && searching){
                        taken = candidates.get(random.nextInt(candidates.size()));
                        tries++;
                        if (queryGraph.contains(taken)){
                            candidates.remove(taken);
                        } else{
                            searching = false;
                        }
                    }

                    if (null == taken){
                        break;
                    }
                    queryGraph.add(taken);
                    candidates.addAll(graph.inEdgesFor(taken.getSource().getId()));
                    candidates.addAll(graph.outEdgesFor(taken.getSource().getId()));
                    candidates.addAll(graph.inEdgesFor(taken.getTarget().getId()));
                    candidates.addAll(graph.outEdgesFor(taken.getTarget().getId()));
                }
                System.out.println("Query created");
                BaseGraph query = makeQuery(queryGraph);
                System.out.println("Copy created");
                injectVariables(query);
                System.out.println("Variables made");
                if(!serializeQuery(query)){
                    i--;
                }
                System.out.println("Written");
            }
        }
    }

    private BaseGraph makeQuery(List<BaseEdge> queryGraph) {
        BaseGraph query = new BaseGraph();
        for (BaseEdge e: queryGraph){
            if (!query.getIdMapping().containsKey(e.getSource().getId())){
                query.addNode(e.getSource().getId(), graph.invertedIndex.get(e.getSource().getId()));
            }
            if (!query.getIdMapping().containsKey(e.getTarget().getId())){
                query.addNode(e.getTarget().getId(), graph.invertedIndex.get(e.getTarget().getId()));
            }
            query.addEdge(e.getSource().getId(), e.getTarget().getId(), e.getLabel());
        }
        return query;
    }

    private void injectVariables(BaseGraph query) {
        int variableCounter = -1;
        List<BaseNode> nodes = new ArrayList<>(query.getNodes());

        int noVariableIndex = random.nextInt(nodes.size());
        int variableIndex;
        do {
            variableIndex = random.nextInt(nodes.size());
        } while (variableIndex == noVariableIndex);
        for (int i = 0; i < nodes.size(); i++){
            if (i == noVariableIndex){
                continue;
            }
            if (i == variableIndex || random.nextDouble() < 1.0 / query.getEdges().size()){

                nodes.get(i).setId(variableCounter);
                query.invertedIndex.put(variableCounter, "*" + (-1*variableCounter--));
            }
        }
    }

    private boolean serializeQuery(BaseGraph query) {
        int nodeCounter = 0;
        int resultSize = graph.query(query).size();

        if (resultSize == 0 || resultSize > 10000){
            return false;
        }

        System.out.println(resultSize);
        String querydir = outputDir + (isTest ? "test/" : "train/") + "query";
        try(PrintStream queryFile = new PrintStream(new File(querydir + queryCounter++))){
            queryFile.println("#" + resultSize);
            for (BaseNode node: query.getNodes()){
                queryFile.println("v " + node.getId() + " " + query.invertedIndex.get(node.getId()));
            }
            for (BaseEdge edge: query.getEdges()){
                queryFile.println("e " + edge.getSource().getId() + " " + edge.getTarget().getId() + " " + edge.getLabel());
            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }
        return true;
    }

    public static void main(String[] args){
        String dataDir = "/home/lukas/studium/thesis/code/data/citation/";
        String graphFile = "graph_3";
        String outputDir = "/home/lukas/studium/thesis/code/data/movie/queriesnew/";
        List<String> seeds = new ArrayList<>();
        seeds.add("aut:danai_koutra");
        QueryGenerator q = new QueryGenerator(dataDir+graphFile, outputDir, seeds);
        q.generate(false);
        q.generate(true);
    }
}
