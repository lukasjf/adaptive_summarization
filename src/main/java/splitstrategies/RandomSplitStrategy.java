package splitstrategies;

import graph.Node;
import graph.summary.Summary;
import graph.summary.SummaryNode;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Created by lukas on 13.03.18.
 */
public class RandomSplitStrategy extends SplitStrategy {


    @Override
    public void split(Summary summary) {
        Random random = new Random();
        SummaryNode splitNode = (SummaryNode) summary.getNodeMapping()
                .get(random.nextInt(summary.getNodeMapping().size()));
        int newNodeId = summary.getNodeMapping().size();
        Set<String> containedNodes = splitNode.getLabels();

        SummaryNode new1 = new SummaryNode(splitNode.getId(), new HashSet<>());
        SummaryNode new2 = new SummaryNode(newNodeId, new HashSet<>());

        for (String label: containedNodes){
            if (random.nextBoolean()){
                new1.getLabels().add(label);
            } else{
                new2.getLabels().add(label);
            }
        }

        adjustLabels(summary, splitNode, new1, new2);
    }
}
