package main;

import graph.*;

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
    private static int QUERIES_PER_SIZE = 50;
    private static int MAX_TRIES = 10000;

    private static int TEST_QUERIES_PER_SIZE = 50;

    private BaseGraph graph;
    private List<BaseGraph> queries;
    private Random random = new Random(42);

    private int queryCounter = 1;

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
        this.queries = new ArrayList<>();

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

    private void addSeedNode(BaseNode node){
        if (!seedNodes.contains(node)){
            seedNodes.add(node);
        }
    }

    private void generate(){
        for (int s = LOWER_SIZE; s <= UPPER_SIZE; s++) {
            int sizeBound = QUERIES_PER_SIZE + TEST_QUERIES_PER_SIZE;
            for (int i = 0; i < sizeBound; i++) {
                int variableCounter = -1;
                Map<BaseNode, BaseNode> graphQueryMapping = new HashMap<>();
                Map<BaseEdge, BaseNode> candidates = new HashMap<>();

                BaseGraph queryGraph = new BaseGraph();

                int startIndex = random.nextInt(seedNodes.size());
                BaseNode startNode = seedNodes.get(startIndex);
                queryGraph.addNode(startNode.getId(), graph.invertedIndex.get(startNode.getId()));
                graphQueryMapping.put(startNode, queryGraph.getIdMapping().get(startNode.getId()));

                graph.inEdgesFor(startNode.getId()).forEach(e -> candidates.put(e, startNode));
                graph.outEdgesFor(startNode.getId()).forEach(e -> candidates.put(e, startNode));

                if (candidates.isEmpty()){
                    i--;
                    System.out.println("bad starting node");
                    continue;
                }

                //randomly choose edge to add
                for (int k = 1; k < s; k++){
                    BaseEdge taken = null;
                    int tries = 0;
                    boolean searching = true;
                    while (!candidates.isEmpty() && tries < MAX_TRIES && searching){
                        List<BaseEdge> chooseFrom = new ArrayList<>(candidates.keySet());
                        taken = chooseFrom.get(random.nextInt(chooseFrom.size()));
                        tries++;
                        if (queryGraph.getEdges().contains(taken)){
                            candidates.remove(taken);
                        } else{
                            searching = false;
                        }
                    }

                    if (null == taken){
                        break;
                    }

                    boolean takenFromSource = taken.getSource().equals(candidates.get(taken));
                    BaseNode otherNode = takenFromSource ? taken.getTarget() : taken.getSource();
                    boolean newVariable = graphQueryMapping.get(candidates.get(taken)).isVariable() ? random.nextBoolean() : true;
                    int newNodeId = newVariable ? variableCounter : otherNode.getId();
                    String newNodeLabel = newVariable ? "*" + variableCounter-- : graph.invertedIndex.get(otherNode.getId());

                    queryGraph.addNode(newNodeId, newNodeLabel);
                    graphQueryMapping.put(otherNode, queryGraph.getIdMapping().get(newNodeId));
                    if (takenFromSource){
                        queryGraph.addEdge(graphQueryMapping.get(taken.getSource()).getId(), newNodeId, taken.getLabel());

                    } else{
                        queryGraph.addEdge(newNodeId, graphQueryMapping.get(taken.getTarget()).getId(), taken.getLabel());
                    }
                    graph.inEdgesFor(otherNode.getId()).forEach(e -> candidates.put(e, otherNode));
                    graph.outEdgesFor(otherNode.getId()).forEach(e -> candidates.put(e, otherNode));
                }

                if (queries.stream().anyMatch(q -> areDuplicateQueries(queryGraph, q))){
                    System.out.println("Query Already Made");
                    continue;
                }

                int resultSize = graph.query(queryGraph).size();
                if (resultSize == 0 || resultSize > 1000){
                    continue;
                }
                System.out.println(resultSize);

                queries.add(queryGraph);
            }
            serializeQueries();
            System.out.println("Size " + s + " queries written");
            queries.clear();
        }
    }

    private boolean areDuplicateQueries(BaseGraph query, BaseGraph oldQuery){
        return new SubgraphIsomorphism().query(oldQuery, query, true).size() > 0 &&
                new SubgraphIsomorphism().query(query, oldQuery, true).size() > 0;
    }


    private void serializeQueries() {
        for (int i = 0; i < queries.size(); i++){
            serializeQuery(queries.get(i), i >= QUERIES_PER_SIZE);
        }

    }


    private void serializeQuery(BaseGraph query, boolean isTest){
        int resultSize = graph.query(query).size();
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
    }

    public static void main(String[] args){
        String dataDir = "/home/lukas/studium/thesis/code/data/citation/";
        String graphFile = "graph_3";
        String outputDir = "/home/lukas/studium/thesis/code/data/citation/queriesnew/";
        List<String> seeds = new ArrayList<>();
        seeds.add("aut:davide_mottin");
        QueryGenerator q = new QueryGenerator(dataDir+graphFile, outputDir, seeds);
        q.generate();
    }
}
