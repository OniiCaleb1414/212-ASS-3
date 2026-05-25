public class Main {

    public static void main(String[] args) {
        Graph graph = new Graph();

        Vertex A = graph.addVertex("smelter");
        Vertex B = graph.addVertex("constructor");
        Vertex D = graph.addVertex("miner");
        Vertex C = graph.addVertex("assembler");
        Vertex E = graph.addVertex("constructor");
        Vertex F = graph.addVertex("manufacturer");
        Vertex G = graph.addVertex("refinery");
        Vertex H = graph.addVertex("smelter");

        graph.addEdge(D, A, "copper", 1);
        graph.addEdge(A, B, "iron", 1);
        graph.addEdge(B, C, "coal", 1);
        graph.addEdge(E, A, "caterium", 1);
        graph.addEdge(G, H, "aluminum", 1);
        graph.addEdge(E, F, "copper", 1);
        graph.addEdge(H, E, "iron", 1);

        Graph MST = graph.MST();
        System.out.println(MST);
        graph.Stage2Optimization();
        System.out.println(graph);
    }
}