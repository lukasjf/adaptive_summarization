package main;

import graph.BaseEdge;
import graph.BaseGraph;
import graph.BaseNode;

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
    private static int MAX_TRIES = 100;

    private String dataDir;
    private BaseGraph graph;
    private Random random = new Random(42);

    private int queryCounter = 1;

    private List<BaseNode> kddNodes = new ArrayList<>();

    public QueryGenerator(String dataDir, String graphFile){
        this.dataDir = dataDir;
        this.graph = BaseGraph.parseGraph(dataDir + graphFile);

        BaseNode kdd = graph.getLabelMapping().get("aut:christos_faloutsos");
        kddNodes.add(kdd);
        for (BaseEdge edge: graph.getInIndex().get(kdd)){
            if (!kddNodes.contains(edge.getSource()))
                kddNodes.add(edge.getSource());
        }
        for (BaseEdge edge: graph.getOutIndex().get(kdd)){
            if (!kddNodes.contains(edge.getTarget()))
                kddNodes.add(edge.getTarget());
        }
        System.out.println(kddNodes.size());
    }

    public void generate(){
        for (int s = LOWER_SIZE; s <= UPPER_SIZE; s++) {
            for (int i = 0; i < QUERIES_PER_SIZE; i++) {
                List<BaseEdge> candidates = new ArrayList<>();
                List<BaseEdge> queryGraph = new ArrayList<>();

                //int startIndex = random.nextInt(graph.getNodeMapping().size());
                //BaseNode startNode = graph.getNodeMapping().get(startIndex);
                int startIndex = random.nextInt(kddNodes.size());
                BaseNode startNode = kddNodes.get(startIndex);
                candidates.addAll(graph.getInIndex().get(startNode));
                candidates.addAll(graph.getInIndex().get(startNode));
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
                    candidates.addAll(graph.getInIndex().get(taken.getSource()));
                    candidates.addAll(graph.getOutIndex().get(taken.getSource()));
                    candidates.addAll(graph.getInIndex().get(taken.getTarget()));
                    candidates.addAll(graph.getOutIndex().get(taken.getTarget()));
                }
                System.out.println("Query created");
                BaseGraph query = makeQuery(queryGraph);
                System.out.println("Copy created");
                injectVariables(query);
                System.out.println("Variables made");
                serializeQuery(query);
                System.out.println("Written");
            }
        }
    }

    private BaseGraph makeQuery(List<BaseEdge> queryGraph) {
        BaseGraph query = new BaseGraph();
        List<BaseEdge> copy = new ArrayList<>();
        for (BaseEdge e: queryGraph){
            if (!query.getNodeMapping().containsKey(e.getSource().getId())){
                query.addNode(e.getSource().getId(), e.getSource().getLabel());
            }
            if (!query.getNodeMapping().containsKey(e.getTarget().getId())){
                query.addNode(e.getTarget().getId(), e.getTarget().getLabel());
            }
            query.addEdge(e.getSource().getId(), e.getTarget().getId(), e.getLabel());
        }
        return query;
    }

    private void injectVariables(BaseGraph query) {
        int variableCounter = 0;
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
                nodes.get(i).setLabel("?" + variableCounter++);
            }
        }
    }

    private void serializeQuery(BaseGraph query) {
        int nodeCounter = 0;
        int resultSize = graph.query(query).size();
        if (resultSize > 1000){
            return;
        }
        System.out.println(resultSize);
        Map<BaseNode, Integer> nodeIDs = new HashMap<>();
        try(PrintStream queryFile = new PrintStream(new File(dataDir + "queriesfaloutsos/query" + queryCounter++))){
            queryFile.println("#" + resultSize);
            for (BaseNode node: query.getNodes()){
                nodeIDs.put(node, nodeCounter);
                queryFile.println("v " + nodeCounter++ + " " + node.getLabel());
            }
            for (BaseEdge edge: query.getEdges()){
                queryFile.println("e " + nodeIDs.get(edge.getSource()) + " " + nodeIDs.get(edge.getTarget()) + " " + edge.getLabel());
            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        QueryGenerator q = new QueryGenerator("/home/lukas/studium/thesis/code/data/citation/","graph_3");
        q.generate();
    }
}
