package graph;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lukas on 19.06.18.
 */
public class RDFParser {

    public static void main(String[] args){

        String rdfFile = args[0];
        String outputFile = args[1];

        Map<String, Integer> nodes = new HashMap<>();
        int nodecounter = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(new File(rdfFile)));
            PrintStream output = new PrintStream(outputFile)) {

            for (String line = br.readLine(); line != null; line = br.readLine()) {
                String[] token = line.split(" ");
                String subject = token[0];
                String property = token[1];
                String object = token[2];

                if (!nodes.containsKey(subject)){
                    output.println(String.format("v %d %s", nodecounter, subject));
                    nodes.put(subject, nodecounter++);
                }

                if (!nodes.containsKey(object)){
                    output.println(String.format("v %d %s", nodecounter, object));
                    nodes.put(object, nodecounter++);
                }

                output.println(String.format("e %d %d %s", nodes.get(subject), nodes.get(object), property));
            }
            output.flush();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
