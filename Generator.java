import java.util.Random;
// !DO NOT CHANGE THIS FILE USED to generate random graphs for testing
public class Generator {

    static Graph generate(int vertices, int edges) { // Static method to generate a random graph with the given number of vertices and edges
        Random r = new Random(67);
        Graph graph = new Graph();

        Vertex[] v = new Vertex[vertices];
        VertexType[] vTypes = VertexType.values();
        EdgeType[] eTypes = EdgeType.values();

        for (int i = 0; i < vertices; i++) {
            VertexType chosen = vTypes[r.nextInt(vTypes.length)];
            if (chosen == VertexType.UNDEFINED) {
                chosen = VertexType.CONSTRUCTOR;
            }
            v[i] = graph.addVertex(chosen.toString().toLowerCase());
        }

        for (int i = 0; i < edges; i++) {
            int a = r.nextInt(vertices);
            int b = r.nextInt(vertices);

            if (a == b) {
                i--;
                continue;
            }

            EdgeType type = eTypes[r.nextInt(eTypes.length)];
            if (type == EdgeType.UNDEFINED) {
                type = EdgeType.CATERIUM;
            }
            double weight = 1 + r.nextDouble() * 9;
            weight = Math.round(weight * 100.0) / 100.0;

            graph.addEdge(v[a], v[b], type.toString().toLowerCase(), weight);
        }
        return graph;
    }
}