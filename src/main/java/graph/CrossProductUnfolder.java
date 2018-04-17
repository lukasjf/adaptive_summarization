package graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by lukas on 17.04.18.
 */
public class CrossProductUnfolder {

    private int[] queryIds;
    private int[][] matchIds;

    private int[] matchIndices;

    private int numberNodes;

    private int totalCount = 1;
    private int currentCount = 0;

    public CrossProductUnfolder(Map<BaseNode, BaseNode> match){
        numberNodes = match.size();

        int index = 0;
        queryIds = new int[numberNodes];
        matchIds = new int[numberNodes][];
        matchIndices = new int[numberNodes];
        for (BaseNode n: match.keySet()){
            queryIds[index] = n.getId();
            if (n.isVariable()){
                Set<Integer> matchResults = match.get(n).containedNodes;
                matchIds[index] = new int[matchResults.size()];
                int j = 0;
                for (int m: matchResults){
                    matchIds[index][j++] = m;
                }
                totalCount *= matchResults.size();
            }
            else{
                matchIds[index] = new int[]{n.getId()};
            }
            index++;
        }

    }

    public boolean hasNext(){
        if (totalCount > 100000){
            // Soft check for too large results (e.g. due to too coarse summary)
            return false;
        }
        return currentCount < totalCount;
    }

    public Map<Integer, Integer> next(){
        Map<Integer, Integer> result = new HashMap<>();
        for (int i = 0; i < numberNodes; i++){
            result.put(queryIds[i], matchIds[i][matchIndices[i]]);
        }
        currentCount++;
        if (hasNext()) {
            int updateIndex = 0;
            while (matchIndices[updateIndex] + 1 == matchIds[updateIndex].length) {
                matchIndices[updateIndex] = 0;
                updateIndex++;
            }
            matchIndices[updateIndex]++;
        }
        return result;
    }
}
