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

    boolean noisy;

    private double fraction;

    private List<BaseNode> focusNodes;
    private List<BaseNode> innerNodes;

    private Set<String> queryNodes = new HashSet<>();

    int seedNumber;

    public QueryGenerator(String input, String output, int queryFromSize, int queryToSize, int queriesPerSize,
                          double graphFraction, boolean noisy, int seedNumber){
        random = new Random((long) ((noisy?1:0) + queryFromSize * queryToSize + queriesPerSize + graphFraction));

        this.fromSize = queryFromSize;
        this.toSize = queryToSize;
        this.numberPerSize = queriesPerSize;
        this.fraction = graphFraction;
        this.noisy = noisy;
        this.seedNumber = seedNumber;

        new Dataset(input);
        this.outputDir = output;
        File outDir = new File(outputDir);
        if (!outDir.exists()){
            outDir.mkdir();
        }
        this.graph = GraphImporter.parseGraph(input);
        this.queries = new ArrayList<>();

        List<BaseNode> starting = new ArrayList<>(graph.getNodes());

        List<BaseNode> seeds = new ArrayList<>();

        for (int i = 0; i < seedNumber; i++){
            BaseNode seedNode = starting.get(random.nextInt(starting.size()));
            int nSize = graph.outEdgesFor(seedNode.getId()).size() + graph.inEdgesFor(seedNode.getId()).size();
            while (Dataset.I.blacklist.contains(seedNode.getId()) || nSize < 5 || seeds.contains(seedNode)){
                seedNode = starting.get(random.nextInt(starting.size()));
                nSize = graph.outEdgesFor(seedNode.getId()).size() + graph.inEdgesFor(seedNode.getId()).size();
            }
            seeds.add(seedNode);
        }
        createEnvironment(seeds);
    }

    private void createEnvironment(List<BaseNode> seeds){
        focusNodes = new ArrayList<>();
        innerNodes = new ArrayList<>();

        for (BaseNode seed: seeds){
            innerNodes.add(seed);
            addNodeNeighbours(seed);

            while (focusNodes.size() < fraction / seedNumber * graph.getNodes().size()){
                BaseNode expandNode = focusNodes.get(random.nextInt(focusNodes.size()));
                while (innerNodes.contains(expandNode) || Dataset.I.blacklist.contains(expandNode.getId())){
                    System.out.print(".");
                    expandNode = focusNodes.get(random.nextInt(focusNodes.size()));
                }
                if (addNodeNeighbours(expandNode)){
                    innerNodes.add(expandNode);
                }
            }
        }
    }

    private boolean addNodeNeighbours(BaseNode node){
        if (!focusNodes.contains(node)) {
            focusNodes.add(node);
        }
        for (BaseEdge e: graph.outEdgesFor(node.getId())){
            if (!focusNodes.contains(e.getTarget())){
                if (focusNodes.size() < fraction / seedNumber * graph.getNodes().size()){
                    focusNodes.add(e.getTarget());
                } else{
                    return false;
                }
            }
        }
        for (BaseEdge e: graph.inEdgesFor(node.getId())){
            if (!focusNodes.contains(e.getSource())){
                if (focusNodes.size() < fraction / seedNumber * graph.getNodes().size()){
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


                    if (!noisy) {
                        List<Map<String, String>> results = graph.query(query);
                        Set<Integer> focusIds = focusNodes.stream().map(BaseNode::getId).collect(Collectors.toSet());
                        if (results.stream().anyMatch(m -> m.values().stream().anyMatch(str -> !focusIds.contains(Dataset.I.IDFrom(str))))) {
                            continue;
                        }
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


        try(PrintStream ps = new PrintStream("focusmovie005")){
            ps.println(focusNodes.stream().map(n->""+n.getId()).collect(Collectors.joining(",")));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        serializeQueries();
        System.out.println(String.format("In total %d nodes out of %f were used", queryNodes.size(), graph.getNodes().size() * fraction));
        Set<String> focusLabels = focusNodes.stream().map(n->Dataset.I.labelFrom(n.getId())).collect(Collectors.toSet());
        Set<String> goodNodes = new HashSet<>(queryNodes);
        goodNodes.retainAll(focusLabels);
        System.out.println(String.format("Out which %d are focuscitation001 nodes: ", queryNodes.size()));
        System.out.println(innerNodes.size() + "    " + focusNodes.size());
    }

    private BaseGraph createQuery(int size) {
        BaseGraph query = new BaseGraph();
        BaseNode startNode;
        if (noisy){
            startNode = focusNodes.get(random.nextInt(focusNodes.size()));
        } else{
            startNode = innerNodes.get(random.nextInt(innerNodes.size()));
        }
        List<BaseEdge> outCandidates = new ArrayList<>();
        List<BaseEdge> inCandidates = new ArrayList<>();
        outCandidates.addAll(graph.outEdgesFor(startNode.getId()).stream().filter(e->
                focusNodes.contains(e.getTarget())).collect(Collectors.toSet()));
        inCandidates.addAll(graph.inEdgesFor(startNode.getId()).stream().filter(e->
                focusNodes.contains(e.getSource())).collect(Collectors.toSet()));
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
                    if (focusNodes.contains(e.getTarget())){
                        outCandidates.add(e);
                    }
                }
                for (BaseEdge e: graph.inEdgesFor(expandEdge.getTarget().getId())){
                    if (focusNodes.contains(e.getSource())){
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
                    if (focusNodes.contains(e.getTarget())){
                        outCandidates.add(e);
                    }
                }
                for (BaseEdge e: graph.inEdgesFor(expandEdge.getSource().getId())){
                    if (focusNodes.contains(e.getSource())){
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
        if (!noisy){
            createCleanVariables(query, -1);
            return;
        }
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

    private void createCleanVariables(BaseGraph query, int vc) {
        if (query.getNodes().stream().anyMatch(n->!focusNodes.contains(n))){
            int i = 0;
        }

        for (BaseNode node: innerNodes){
            if (graph.outEdgesFor(node.getId()).stream().anyMatch(e -> !focusNodes.contains(e.getTarget()))){
                int i = 0;
            }
            if (graph.inEdgesFor(node.getId()).stream().anyMatch(e -> !focusNodes.contains(e.getSource()))){
                int k = 0;
            }
        }

        int variableCounter = vc;
        Set<BaseNode> testedNodes = new HashSet<>();

        List<BaseEdge> edges = new ArrayList<>(query.getEdges());
        Collections.shuffle(edges);
        for (BaseEdge e: edges){
            if (random.nextBoolean()){
                //check sources first
                if (innerNodes.contains(e.getTarget())){
                    if (!testedNodes.contains(e.getSource()) && random.nextBoolean()){
                        e.getSource().setId(variableCounter--);
                    }
                    testedNodes.add(e.getSource());
                }
                if (innerNodes.contains(e.getSource())){
                    if (!testedNodes.contains(e.getTarget())&& random.nextBoolean()){
                        e.getTarget().setId(variableCounter--);
                    }
                    testedNodes.add(e.getTarget());
                }
            } else{
                //check targets first
                if (innerNodes.contains(e.getSource())){
                    if (!testedNodes.contains(e.getTarget())&& random.nextBoolean()){
                        e.getTarget().setId(variableCounter--);
                    }
                    testedNodes.add(e.getTarget());
                }
                if (innerNodes.contains(e.getTarget())){
                    if (!testedNodes.contains(e.getSource()) && random.nextBoolean()){
                        e.getSource().setId(variableCounter--);
                    }
                    testedNodes.add(e.getSource());
                }
            }
        }
        if (query.getNodes().stream().noneMatch(n-> n.getId() < 0)){
            createCleanVariables(query,variableCounter);
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
        Set<Integer> focusIds = focusNodes.stream().map(BaseNode::getId).collect(Collectors.toSet());
        if (results.stream().anyMatch(m -> m.values().stream().anyMatch(s-> !focusIds.contains(Dataset.I.IDFrom(s))))){
            int i = 0;
        }
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
        boolean noisy = Boolean.parseBoolean(args[6]);
        int numberseeds = Integer.parseInt(args[7]);
        QueryGenerator q = new QueryGenerator(dataFile, outputDir, queryFromSize, queryToSize, queriesPerSize, graphFraction, noisy, numberseeds);
        q.generate();
    }


}
