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

    private Map<Integer, Integer> nextResult;

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

        createNextResult();
    }

    public boolean hasNext(){
        return nextResult != null;
    }

    private void createNextResult(){

        boolean created = false;
        if (currentCount >= totalCount){
            nextResult = null;
            return;
        }

        Map<Integer, Integer> result;
        while(!created){
            if (currentCount >= totalCount){
                nextResult = null;
                return;
            }

            result = new HashMap<>();
            boolean isInjective = true;
            for (int i = 0; i < numberNodes; i++){
                if(result.values().contains(matchIds[i][matchIndices[i]])){
                    isInjective = false;
                    break;
                } else{
                    result.put(queryIds[i], matchIds[i][matchIndices[i]]);
                }
            }
            if (isInjective){
                nextResult = result;
                created = true;
            }
            incrementIndices();
        }
    }

    public Map<Integer, Integer> next(){
        Map<Integer, Integer> result = nextResult;
        createNextResult();
        return result;
    }

    private void incrementIndices(){
        currentCount++;
        if (currentCount < totalCount) {
            int updateIndex = 0;
            while (matchIndices[updateIndex] + 1 == matchIds[updateIndex].length) {
                matchIndices[updateIndex] = 0;
                updateIndex++;
            }
            matchIndices[updateIndex]++;
        }
    }
}
