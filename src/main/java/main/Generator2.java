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
public class Generator2 {

    private static int MAX_QUERY_RESULTS = 100;
    private static int tries = 10000;

    private BaseGraph graph;
    private List<BaseGraph> queries;
    private Random random;

    private int queryCounter = 1;

    private String outputDir;

    private List<BaseNode> seedNodes = new ArrayList<>();
    private Map<BaseNode, Double> residuals = new HashMap<>();
    private Map<BaseNode, Double> values = new HashMap<>();

    private int numberPerSize;

    boolean noisy;
    private long seed;

    private double fraction;

    private List<BaseNode> focusNodes = new ArrayList<>();
    private List<BaseNode> innerNodes = new ArrayList<>();

    private Set<String> queryNodes = new HashSet<>();

    int seedNumber;

    public Generator2(String input, String output, int queriesPerSize,
                          double graphFraction, boolean noisy, int seedNumber){
        long seedValue = 1L;
        random = new Random(seedValue);

        this.numberPerSize = queriesPerSize;
        this.fraction = graphFraction;
        this.noisy = noisy;
        this.seedNumber = seedNumber;

        new Dataset(input);
        this.outputDir = output + seedValue + "/";
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

        double alpha = 0.85;
        double tol = 1.0E-5;

        List<BaseNode> queue = new ArrayList<>();

        for (BaseNode seed: seeds){
            residuals.put(seed, 1.0 / seeds.size());
            queue.add(seed);
        }
        while (!queue.isEmpty()){
            BaseNode node = queue.remove(0);
            values.put(node, values.getOrDefault(node, 0.0) + residuals.get(node) * (1-alpha));
            Set<BaseNode> neighbors = getNeighbors(node);
            int degree = neighbors.size();
            double mass = alpha * residuals.get(node) / 2.0 / degree;
            for (BaseNode n: neighbors){
                if (node == n){
                    System.err.println("self loop");
                    System.exit(1);
                }
                if (!residuals.containsKey(n)){
                    residuals.put(n, 0.0);
                }
                double threshhold = getNeighbors(n).size() * tol;
                if (residuals.get(n) < threshhold && residuals.get(n) + mass >= threshhold){
                    queue.add(n);
                }
                residuals.put(n, residuals.get(n) + mass);
            }
            residuals.put(node, mass * degree);
            if (residuals.get(node) >= degree * tol){
                queue.add(node);
            }
        }
        for (BaseNode node: residuals.keySet()){
            values.put(node, values.getOrDefault(node, 0.0) + residuals.get(node));
        }
        PriorityQueue<BaseNode> pq = new PriorityQueue<>(Comparator.comparingDouble(o -> values.getOrDefault(o, 0.0)));
        pq.addAll(residuals.keySet());
        for (int i = 0; i < Dataset.I.getGraph().getNodes().size() * fraction; i++){
            focusNodes.add(pq.poll());
        }

        for (BaseNode node: focusNodes){
            if (Dataset.I.getGraph().outEdgesFor(node.getId()).stream().map(BaseEdge::getTarget).allMatch(focusNodes::contains)){
                if (Dataset.I.getGraph().inEdgesFor(node.getId()).stream().map(BaseEdge::getSource).allMatch(focusNodes::contains)){
                    innerNodes.add(node);
                }
            }
        }
    }

    private Set<BaseNode> getNeighbors(BaseNode node) {
        Set<BaseNode> nodes = Dataset.I.getGraph().outEdgesFor(node.getId()).stream()
                .map(BaseEdge::getTarget).collect(Collectors.toSet());
        nodes.addAll(Dataset.I.getGraph().inEdgesFor(node.getId()).stream()
                .map(BaseEdge::getSource).collect(Collectors.toList()));
        return nodes;
    }

    private BaseNode getRandomNode(){
        double valSum = focusNodes.stream().map(n->values.get(n)).mapToDouble(d->d).sum();
        double r = random.nextDouble() * valSum;
        double running = 0.0;
        BaseNode node = null;
        for (BaseNode n: focusNodes){
            node = n;
            running += values.get(n);
            if (running >= r){
                return n;
            }
        }
        return node;
    }

    private void generate(){
        int[] sizes = new int[]{2,2,2,2,2,2,2,3,4,5};
        for (int i = 0; i < numberPerSize; i++){
            attempt:
            for (int run = 0; run < tries; run++){
                int size = sizes[random.nextInt(sizes.length)];
                BaseGraph query = new BaseGraph();
                BaseNode starting = getRandomNode();
                BaseNode next = starting;
                query.addNode(starting.getId(), Dataset.I.labelFrom(starting.getId()));
                for (int k = 1; k < size; k++){
                    List<BaseEdge> candidates = Dataset.I.getGraph().outEdgesFor(next.getId()).stream().filter(e ->
                        focusNodes.contains(e.getTarget())).collect(Collectors.toList());
                    candidates.addAll(Dataset.I.getGraph().inEdgesFor(next.getId()).stream().filter(e ->
                        focusNodes.contains(e.getSource())).collect(Collectors.toList()));
                    if (candidates.isEmpty()){
                        continue attempt;
                    }

                    BaseEdge expanding = candidates.get(random.nextInt(candidates.size()));
                    while (expanding.getSource() == expanding.getTarget()){
                        candidates.get(random.nextInt(candidates.size()));
                    }
                    BaseNode other = expanding.getSource() == next ? expanding.getTarget() : expanding.getSource();
                    query.addNode(other.getId(), Dataset.I.labelFrom(other.getId()));
                    BaseNode sourceNode = expanding.getSource() == next ? next : other;
                    BaseNode targetNode = expanding.getTarget() == next ? next : other;
                    query.addEdge(sourceNode.getId(), targetNode.getId(), expanding.getLabel());
                    next = other;
                }

                createVariables(query);
                List<Map<String, String>> results = graph.query(query, 10);
                if (results.isEmpty() || results.size() > MAX_QUERY_RESULTS){
                    continue;
                }
                System.out.print(".");
                queries.add(query);
                break;
            }

        }




        try(PrintStream ps = new PrintStream(outputDir + "focusset")){
            ps.println(focusNodes.stream().map(n->""+n.getId()).collect(Collectors.joining(",")));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        serializeQueries();
        System.out.println(String.format("In total %d nodes out of %f were used", queryNodes.size(), graph.getNodes().size() * fraction));
        Set<String> focusLabels = focusNodes.stream().map(n->Dataset.I.labelFrom(n.getId())).collect(Collectors.toSet());
        Set<String> goodNodes = new HashSet<>(queryNodes);
        goodNodes.retainAll(focusLabels);
        System.out.println(String.format("Out which %d are focus nodes: ", goodNodes.size()));
        System.out.println(innerNodes.size() + "    " + focusNodes.size());
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
        int queriesPerSize = Integer.parseInt(args[2]);
        double graphFraction = Double.parseDouble(args[3]);
        boolean noisy = Boolean.parseBoolean(args[4]);
        int numberseeds = Integer.parseInt(args[5]);
        Generator2 q = new Generator2(dataFile, outputDir, queriesPerSize, graphFraction, noisy, numberseeds);
        q.generate();
    }
}
