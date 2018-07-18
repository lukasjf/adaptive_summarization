package main;

import graph.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lukas on 19.03.18.
 */
public class QueryGenerator {

    private static int MAX_QUERY_RESULTS = 100;
    private static int tries = 10000;

    private BaseGraph graph;
    private List<BaseGraph> queries;
    private Random random;

    private int queryCounter = 1;

    private String outputDir;

    private List<BaseNode> seedNodes = new ArrayList<>();

    private int fromSize;
    private int toSize;
    private int numberPerSize;

    private double fraction;

    private List<BaseNode> focusNodes;
    private List<BaseNode> innerNodes;

    private Set<String> queryNodes = new HashSet<>();

    public QueryGenerator(String input, String output, int queryFromSize, int queryToSize, int queriesPerSize, double graphFraction, String seed){
        random = new Random((long) (1 + queryFromSize * queryToSize * queriesPerSize / graphFraction));

        this.fromSize = queryFromSize;
        this.toSize = queryToSize;
        this.numberPerSize = queriesPerSize;
        this.fraction = graphFraction;

        new Dataset(input);
        this.outputDir = output;
        File outDir = new File(outputDir);
        if (!outDir.exists()){
            outDir.mkdir();
        }
        this.graph = GraphImporter.parseGraph(input);
        this.queries = new ArrayList<>();

        if (seed.isEmpty()){
            List<Integer> nodes = new ArrayList<>(graph.getIdMapping().keySet());
            seed = Dataset.I.labelFrom(nodes.get(random.nextInt(nodes.size())));
        }

        BaseNode seedNode = graph.getLabelMapping().get(seed);
        createEnvironment(seedNode);
    }

    private void createEnvironment(BaseNode seed){
        focusNodes = new ArrayList<>();
        innerNodes = new ArrayList<>();

        addNodeNeighbours(seed, focusNodes);

        while (focusNodes.size() < fraction * graph.getNodes().size()){
            BaseNode expandNode = focusNodes.get(random.nextInt(focusNodes.size()));
            while (innerNodes.contains(expandNode) || graph.outEdgesFor(expandNode.getId()).size() > fraction * 0.1 * graph.getNodes().size()){
                expandNode = focusNodes.get(random.nextInt(focusNodes.size()));
            }
            if (addNodeNeighbours(expandNode, focusNodes)){
                innerNodes.add(expandNode);
            }
        }
    }

    private boolean addNodeNeighbours(BaseNode node, List<BaseNode> focusNodes){
        focusNodes.add(node);
        for (BaseEdge e: graph.outEdgesFor(node.getId())){
            if (!focusNodes.contains(e.getTarget())){
                if (focusNodes.size() < fraction * graph.getNodes().size()){
                    focusNodes.add(e.getTarget());
                } else{
                    return false;
                }
            }
        }
        for (BaseEdge e: graph.inEdgesFor(node.getId())){
            if (!focusNodes.contains(e.getSource())){
                if (focusNodes.size() < fraction * graph.getNodes().size()){
                    focusNodes.add(e.getSource());
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    private void generate(){
        for (int s = fromSize; s <= toSize; s++) {
            for (int i = 0; i < numberPerSize; i++) {
                for (int run = 0; run < tries ; run++){
                    BaseGraph query = createQuery(s);

                    if (queries.stream().anyMatch(q -> areDuplicateQueries(query, q))){
                        System.out.println("Query Already Made");
                        continue;
                    }

                    int resultSize = graph.query(query,10).size();
                    if (resultSize == 0 || resultSize > MAX_QUERY_RESULTS){
                        continue;
                    }
                    System.out.println(resultSize);

                    queries.add(query);
                    break;
                }
            }
            System.out.println("Size " + s + " queries done");
        }
        serializeQueries();
        System.out.println(String.format("In total %d nodes out of %f were used", queryNodes.size(), graph.getNodes().size() * fraction));
        System.out.println(innerNodes.size() + "    " + focusNodes.size());
    }

    private BaseGraph createQuery(int size) {
        BaseGraph query = new BaseGraph();
        BaseNode startNode = focusNodes.get(random.nextInt(focusNodes.size()));
        List<BaseEdge> outCandidates = new ArrayList<>();
        List<BaseEdge> inCandidates = new ArrayList<>();
        outCandidates.addAll(graph.outEdgesFor(startNode.getId()));
        inCandidates.addAll(graph.inEdgesFor(startNode.getId()));
        query.addNode(startNode.getId(), Dataset.I.labelFrom(startNode.getId()));

        while (query.getNodes().size() < size){
            if (outCandidates.isEmpty() && inCandidates.isEmpty()){
                System.err.println("node candidates");
                return createQuery(size);
            }
            boolean isOutEdge = random.nextBoolean();
            if (isOutEdge){
                if (outCandidates.isEmpty()){
                    return createQuery(size);
                }
                BaseEdge expandEdge = outCandidates.get(random.nextInt(outCandidates.size()));
                query.addNode(expandEdge.getTarget().getId(), Dataset.I.labelFrom(expandEdge.getTarget().getId()));
                query.addEdge(expandEdge.getSource().getId(), expandEdge.getTarget().getId(), expandEdge.getLabel());
                for (BaseEdge e: graph.outEdgesFor(expandEdge.getTarget().getId())){
                    if (focusNodes.contains(e.getTarget()) && !outCandidates.contains(e)){
                        outCandidates.add(e);
                    }
                }
                for (BaseEdge e: graph.inEdgesFor(expandEdge.getTarget().getId())){
                    if (focusNodes.contains(e.getSource()) && !inCandidates.contains(e)){
                        inCandidates.add(e);
                    }
                }
                inCandidates.remove(expandEdge);
            } else{
                if (inCandidates.isEmpty()){
                    return createQuery(size);
                }
                BaseEdge expandEdge = inCandidates.get(random.nextInt(inCandidates.size()));
                query.addNode(expandEdge.getSource().getId(), Dataset.I.labelFrom(expandEdge.getSource().getId()));
                query.addEdge(expandEdge.getSource().getId(), expandEdge.getTarget().getId(), expandEdge.getLabel());
                for (BaseEdge e: graph.outEdgesFor(expandEdge.getSource().getId())){
                    if (focusNodes.contains(e.getTarget()) && !outCandidates.contains(e)){
                        outCandidates.add(e);
                    }
                }
                for (BaseEdge e: graph.inEdgesFor(  expandEdge.getSource().getId())){
                    if (focusNodes.contains(e.getSource()) && !inCandidates.contains(e)){
                        inCandidates.add(e);
                    }
                }
                outCandidates.remove(expandEdge);
            }
        }

        createVariables(query);
        return query;
    }

    private void createVariables(BaseGraph query) {
        int variableCounter = -1;
        Set<BaseNode> testedNodes = new HashSet<>();


        List<BaseNode> queryNodes = new ArrayList<>(query.getNodes());
        BaseNode variableNode = queryNodes.get(random.nextInt(queryNodes.size()));
        variableNode.setId(variableCounter--);
        testedNodes.add(variableNode);


        for (BaseEdge e: query.getEdges()){
            if (!testedNodes.contains(e.getTarget())){
                if (random.nextBoolean()){
                    e.getTarget().setId(variableCounter--);
                }
                testedNodes.add(e.getTarget());
            }
            if (!testedNodes.contains(e.getSource())){
                if (random.nextBoolean()){
                    e.getSource().setId(variableCounter--);
                }
                testedNodes.add(e.getSource());
            }
        }
    }

    private boolean areDuplicateQueries(BaseGraph query, BaseGraph oldQuery){
        return new SubgraphIsomorphism().query(oldQuery, query, true).size() > 0 &&
                new SubgraphIsomorphism().query(query, oldQuery, true).size() > 0;
    }


    private void serializeQueries() {
        Collections.shuffle(queries);
        for (int i = 0; i < queries.size(); i++){
            serializeQuery(queries.get(i));
            System.out.print(".");
        }

    }


    private void serializeQuery(BaseGraph query){
        List<Map<String,String>> results = graph.query(query);
        for (Map<String, String> res: results){
            queryNodes.addAll(res.values());
        }

        int resultSize = results.size();
        String querydir = outputDir;
        try(PrintStream queryFile = new PrintStream(new File(querydir + queryCounter++))){
            queryFile.println("#" + resultSize);
            for (BaseNode node: query.getNodes()){
                queryFile.println("v " + node.getId() + " " + Dataset.I.labelFrom(node.getId()));
            }
            for (BaseEdge edge: query.getEdges()){
                queryFile.println("e " + edge.getSource().getId() + " " + edge.getTarget().getId() + " " + edge.getLabel());
            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        String dataFile = args[0];
        String outputDir = args[1];
        int queryFromSize = Integer.parseInt(args[2]);
        int queryToSize = Integer.parseInt(args[3]);
        int queriesPerSize = Integer.parseInt(args[4]);
        double graphFraction = Double.parseDouble(args[5]);
        String seed = "";
        QueryGenerator q = new QueryGenerator(dataFile, outputDir, queryFromSize, queryToSize, queriesPerSize, graphFraction, seed);
        q.generate();
    }


}
