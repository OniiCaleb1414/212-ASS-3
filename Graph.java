
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;

public class Graph {
    private final ArrayList<Vertex> vertices;

    public Graph(String filename) {
        
    }

    public Graph() {

    }

    public void Stage1Optimization() {

    }

    public void Stage2Optimization() {

    }

    public void Stage3Optimization() {

    }

    public void Stage4Optimization() {

    }

    public void optimize() {

    }

    public Graph MST() {

    }

    public Graph[] SCC() {

    }

    public Vertex addVertex(String type) {

    }

    public void addEdge(Vertex v1, Vertex v2, String type, double weight) {
    
    }

    public Vertex getVertex(int counter) {

    }

    // ! DO NOT MODIFY THE FOLLOWING METHODS.

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        HashSet<Integer> ids = new HashSet<>();

        for (Vertex vertex : vertices) {
            for (Edge edge : vertex.edges) {
                sb.append(edge.toString()).append(":").append(edge.v1).append("->").append(edge.v2).append("\n");
                if (!ids.contains(edge.v2.counter)) {
                    ids.add(edge.v2.counter);
                }
            }
            if (!vertex.edges.isEmpty() && !ids.contains(vertex.counter)) {
                ids.add(vertex.counter);
            }
        }

        for (Vertex vertex : vertices) {
            if (!ids.contains(vertex.counter)) {
                sb.append(vertex.toString()).append("\n");
            }
        }

        if (sb.length() == 0) {
            return "Empty graph";
        }

        return sb.toString();
    }

    public void printToFile() {
        String timeStamp = Long.toString(System.currentTimeMillis(), 36);
        timeStamp = timeStamp.substring(timeStamp.length() - 4);
        String text = toString();
        try {
            Files.write(Paths.get("output/" + timeStamp + ".graph"), text.getBytes());
            System.out.println("Graph written to output/" + timeStamp + ".graph");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
