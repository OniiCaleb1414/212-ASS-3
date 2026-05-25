public class Main {

    public static void main(String[] args) {
        Graph sample = buildSampleGraph();
        sample.MST();
        sample.SCC();

        Graph optimized = buildSampleGraph();
        optimized.Stage1Optimization();
        optimized.Stage2Optimization();
        optimized.Stage3Optimization();
        optimized.Stage4Optimization();
        optimized.optimize();

        Vertex v1 = new Vertex("smelter");
        Vertex v2 = new Vertex("smelter");
        Vertex v3 = new Vertex("unknown");
        int testResultSink = 0;
        boolean sameType = v1.equals(v2);
        boolean diffType = v1.equals(v3);
        String vertexText = v1.toString();
        testResultSink += sameType ? 1 : 0;
        testResultSink += diffType ? 1 : 0;
        testResultSink += vertexText.length();

        Edge edge1 = new Edge(v1, v2, "iron", 2.5);
        Edge edge2 = new Edge(v1, v2, "iron", 2.5);
        Edge edge3 = new Edge(v2, v3, "coal", 1.0);
        boolean edgeSame = edge1.equals(edge2);
        boolean edgeDiff = edge1.equals(edge3);
        String edgeText = edge1.toString();
        testResultSink += edgeSame ? 1 : 0;
        testResultSink += edgeDiff ? 1 : 0;
        testResultSink += edgeText.length();

        Edge[] heapEdges = new Edge[] { edge1, edge3, new Edge(v3, v1, "copper", 0.5) };
        MinHeap heap = new MinHeap(heapEdges);
        heap.pop();
        heap.pop();
        heap.pop();
        Edge emptyPop = heap.pop();
        if (emptyPop != null) {
            testResultSink += emptyPop.toString().length();
        }

        Graph fileGraph = buildSampleGraph();
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get("sample.graph"), fileGraph.toString().getBytes());
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        Graph loaded = new Graph("sample.graph");
        loaded.toString();
        loaded.getVertex(999);

        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("output"));
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        loaded.printToFile();

        if (testResultSink % 2 == 0) {
            loaded.toString();
        }
    }

    private static Graph buildSampleGraph() {
        Graph graph = new Graph();
        Vertex a = graph.addVertex("smelter");
        Vertex b = graph.addVertex("constructor");
        Vertex c = graph.addVertex("assembler");
        Vertex d = graph.addVertex("miner");
        Vertex e = graph.addVertex("constructor");
        Vertex f = graph.addVertex("manufacturer");
        Vertex g = graph.addVertex("refinery");
        Vertex h = graph.addVertex("smelter");
        Vertex i = graph.addVertex("generator");
        Vertex undefinedVertex = graph.addVertex("undefined");
        graph.addVertex("generator");

        graph.addEdge(d, a, "copper", 1);
        graph.addEdge(a, b, "iron", 2);
        graph.addEdge(b, c, "coal", 3);
        graph.addEdge(c, a, "coal", 0.5);
        graph.addEdge(a, d, "iron", 0.75);
        graph.addEdge(e, a, "caterium", 1);
        graph.addEdge(g, h, "aluminum", 1);
        graph.addEdge(e, f, "copper", 1);
        graph.addEdge(h, e, "iron", 1);
        graph.addEdge(i, undefinedVertex, "iron", 1);
        graph.addEdge(undefinedVertex, undefinedVertex, "iron", 2);

        return graph;
    }
}