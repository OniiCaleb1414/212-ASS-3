import java.nio.file.Files;
import java.nio.file.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.HashMap;

public class Graph {
    private final ArrayList<Vertex> vertices;

    public Graph() {
        this.vertices = new ArrayList<>();
    }

    public Graph(String filename) {
        this.vertices = new ArrayList<>();
        fromFile(filename);
    }

    public void Stage1Optimization() {
        for (Vertex vertex : vertices) {
            if (vertex.type == VertexType.MINER) {
                vertex.edges.clear();
            }
        }

        for (Vertex vertex : vertices) {
            if (!vertex.edges.isEmpty()) {
                Edge maxEdge = Collections.max(vertex.edges, Comparator.comparingDouble(e -> e.weight));
                ArrayList<Edge> edgesToRemove = new ArrayList<>();
                for (Edge edge : vertex.edges) {
                    if (edge.weight < maxEdge.weight ||
                       (edge.weight == maxEdge.weight && !edge.equals(maxEdge))) {
                        edgesToRemove.add(edge);
                    }
                }
                vertex.edges.removeAll(edgesToRemove);
            }
        }
    }

    public void Stage2Optimization() {
        IdentityHashMap<Vertex, Vertex> vertexMap = new IdentityHashMap<>();

        for (Vertex v : vertices) {
            boolean mapped = false;
            for (Vertex mappedVertex : vertexMap.keySet()) {
                if (mappedVertex.type == v.type) {
                    vertexMap.put(v, mappedVertex);
                    mapped = true;
                    break;
                }
            }
            if (!mapped) {
                vertexMap.put(v, v);
            }
        }

        HashSet<String> processedEdges = new HashSet<>();
        ArrayList<Edge> edgesToRemove = new ArrayList<>();

        for (Vertex source : vertices) {
            for (Edge edge : source.edges) {
                Vertex mappedSource = vertexMap.get(source);
                Vertex mappedDest = vertexMap.get(edge.v2);

                String edgeKey = mappedSource.counter + "," + mappedDest.counter + "," + edge.type;

                if (processedEdges.contains(edgeKey)) {
                    edgesToRemove.add(edge);

                    for (Vertex v : vertices) {
                        for (Edge e : v.edges) {
                            Vertex origSource = vertexMap.get(v);
                            Vertex origDest = vertexMap.get(e.v2);
                            String origKey = origSource.counter + "," + origDest.counter + "," + e.type;
                            if (origKey.equals(edgeKey)) {
                                e.weight += edge.weight;
                                break;
                            }
                        }
                    }
                } else {
                    processedEdges.add(edgeKey);
                }
            }
        }

        for (Vertex vertex : vertices) {
            vertex.edges.removeAll(edgesToRemove);
        }
    }

    public void Stage3Optimization() {
        for (Vertex vertex : vertices) {
            ArrayList<Edge> selfLoops = new ArrayList<>();
            for (Edge edge : vertex.edges) {
                if (edge.v1 == edge.v2) {
                    selfLoops.add(edge);
                }
            }
            vertex.edges.removeAll(selfLoops);
        }
    }

    public void Stage4Optimization() {
        ArrayList<Vertex> isolatedVertices = new ArrayList<>();
        for (Vertex vertex : vertices) {
            if (vertex.edges.isEmpty()) {
                for (Vertex v : vertices) {
                    ArrayList<Edge> incomingEdges = new ArrayList<>();
                    for (Edge edge : v.edges) {
                        if (edge.v2 == vertex) {
                            incomingEdges.add(edge);
                        }
                    }
                    v.edges.removeAll(incomingEdges);
                }
                isolatedVertices.add(vertex);
            }
        }
        vertices.removeAll(isolatedVertices);
    }

    public void optimize() {
        Stage1Optimization();
        Stage2Optimization();
        Stage3Optimization();
        Stage4Optimization();
    }

    public Graph MST() {
        Graph undirectedGraph = new Graph();

        for (Vertex vertex : vertices) {
            Vertex newVertex = new Vertex(vertex.type.toString().toLowerCase());
            undirectedGraph.vertices.add(newVertex);
        }

        HashMap<Vertex, Vertex> vertexMap = new HashMap<>();
        for (int i = 0; i < vertices.size(); i++) {
            vertexMap.put(vertices.get(i), undirectedGraph.vertices.get(i));
        }

        for (Vertex vertex : vertices) {
            Vertex newVertex = vertexMap.get(vertex);
            for (Edge edge : vertex.edges) {
                Vertex newV1 = vertexMap.get(edge.v1);
                Vertex newV2 = vertexMap.get(edge.v2);

                undirectedGraph.addEdge(newV1, newV2, edge.type.toString(), edge.weight);
                undirectedGraph.addEdge(newV2, newV1, edge.type.toString(), edge.weight);
            }
        }

        Graph mst = new Graph();

        for (Vertex vertex : undirectedGraph.vertices) {
            mst.vertices.add(new Vertex(vertex.type.toString().toLowerCase()));
        }

        HashMap<Vertex, Vertex> mstVertexMap = new HashMap<>();
        for (int i = 0; i < undirectedGraph.vertices.size(); i++) {
            mstVertexMap.put(undirectedGraph.vertices.get(i), mst.vertices.get(i));
        }

        ArrayList<Edge> allEdges = new ArrayList<>();
        boolean[][] edgeAdded = new boolean[undirectedGraph.vertices.size()][undirectedGraph.vertices.size()];

        for (int i = 0; i < undirectedGraph.vertices.size(); i++) {
            Vertex vertex = undirectedGraph.vertices.get(i);
            for (Edge edge : vertex.edges) {
                int v1Index = undirectedGraph.vertices.indexOf(edge.v1);
                int v2Index = undirectedGraph.vertices.indexOf(edge.v2);
                if (v1Index < v2Index && !edgeAdded[v1Index][v2Index]) {
                    allEdges.add(edge);
                    edgeAdded[v1Index][v2Index] = true;
                    edgeAdded[v2Index][v1Index] = true;
                }
            }
        }

        Collections.sort(allEdges, new Comparator<Edge>() {
            @Override
            public int compare(Edge e1, Edge e2) {
                if (Double.compare(e1.weight, e2.weight) != 0) {
                    return Double.compare(e1.weight, e2.weight);
                }
                int v1Comp = Integer.compare(e1.v1.counter, e2.v1.counter);
                if (v1Comp != 0) {
                    return v1Comp;
                }
                return Integer.compare(e1.v2.counter, e2.v2.counter);
            }
        });

        int[] parent = new int[mst.vertices.size()];
        int[] rank = new int[mst.vertices.size()];

        for (int i = 0; i < mst.vertices.size(); i++) {
            parent[i] = i;
        }

        java.util.function.Function<Integer, Integer> find = new java.util.function.Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer x) {
                if (parent[x] != x) {
                    parent[x] = apply(parent[x]);
                }
                return parent[x];
            }
        };

        java.util.function.BiConsumer<Integer, Integer> union = (x, y) -> {
            int xRoot = find.apply(x);
            int yRoot = find.apply(y);
            if (xRoot == yRoot) return;
            if (rank[xRoot] < rank[yRoot]) {
                parent[xRoot] = yRoot;
            } else if (rank[xRoot] > rank[yRoot]) {
                parent[yRoot] = xRoot;
            } else {
                parent[yRoot] = xRoot;
                rank[xRoot]++;
            }
        };

        for (Edge edge : allEdges) {
            int v1Index = undirectedGraph.vertices.indexOf(edge.v1);
            int v2Index = undirectedGraph.vertices.indexOf(edge.v2);

            if (find.apply(v1Index) != find.apply(v2Index)) {
                union.accept(v1Index, v2Index);

                Vertex mstV1 = mstVertexMap.get(undirectedGraph.vertices.get(v1Index));
                Vertex mstV2 = mstVertexMap.get(undirectedGraph.vertices.get(v2Index));
                mst.addEdge(mstV1, mstV2, edge.type.toString(), edge.weight);
            }
        }

        return mst;
    }

    public Graph[] SCC() {
        ArrayList<Vertex> finishOrder = new ArrayList<>();
        HashSet<Vertex> visited = new HashSet<>();

        for (Vertex vertex : vertices) {
            if (!visited.contains(vertex)) {
                dfsFillOrder(vertex, visited, finishOrder);
            }
        }

        Graph transpose = new Graph();
        for (Vertex vertex : vertices) {
            transpose.vertices.add(new Vertex(vertex.type.toString().toLowerCase()));
        }

        HashMap<Vertex, Vertex> transposeVertexMap = new HashMap<>();
        for (int i = 0; i < vertices.size(); i++) {
            transposeVertexMap.put(vertices.get(i), transpose.vertices.get(i));
        }

        for (Vertex vertex : vertices) {
            Vertex newVertex = transposeVertexMap.get(vertex);
            for (Edge edge : vertex.edges) {
                Vertex newV1 = transposeVertexMap.get(edge.v2); // reverse edge
                Vertex newV2 = transposeVertexMap.get(edge.v1); // reverse edge
                transpose.addEdge(newV1, newV2, edge.type.toString(), edge.weight);
            }
        }

        visited.clear();
        ArrayList<Graph> sccList = new ArrayList<>();

        for (int i = finishOrder.size() - 1; i >= 0; i--) {
            Vertex vertex = finishOrder.get(i);
            if (!visited.contains(vertex)) {
                Graph scc = new Graph();
                HashMap<Vertex, Vertex> sccVertexMap = new HashMap<>();

                ArrayList<Vertex> sccVertices = new ArrayList<>();
                dfsCollectSCC(vertex, visited, sccVertices, sccVertexMap);

                for (Vertex v : sccVertices) {
                    scc.vertices.add(new Vertex(v.type.toString().toLowerCase()));
                }

                HashMap<Vertex, Vertex> finalVertexMap = new HashMap<>();
                for (int j = 0; j < sccVertices.size(); j++) {
                    finalVertexMap.put(sccVertices.get(j), scc.vertices.get(j));
                }

                for (Vertex v : sccVertices) {
                    Vertex newV = finalVertexMap.get(v);
                    for (Edge edge : v.edges) {
                        if (sccVertices.contains(edge.v2)) { // only edges within SCC
                            Vertex newV2 = finalVertexMap.get(edge.v2);
                            scc.addEdge(newV, newV2, edge.type.toString(), edge.weight);
                        }
                    }
                }

                sccList.add(scc);
            }
        }

        return sccList.toArray(new Graph[0]);
    }

    private void dfsFillOrder(Vertex vertex, HashSet<Vertex> visited, ArrayList<Vertex> finishOrder) {
        visited.add(vertex);
        for (Edge edge : vertex.edges) {
            if (!visited.contains(edge.v2)) {
                dfsFillOrder(edge.v2, visited, finishOrder);
            }
        }
        finishOrder.add(vertex);
    }

    private void dfsCollectSCC(Vertex vertex, HashSet<Vertex> visited, ArrayList<Vertex> sccVertices, HashMap<Vertex, Vertex> vertexMap) {
        visited.add(vertex);
        sccVertices.add(vertex);
        for (Edge edge : vertex.edges) {
            if (!visited.contains(edge.v2)) {
                dfsCollectSCC(edge.v2, visited, sccVertices, vertexMap);
            }
        }
    }

    public Vertex addVertex(String type) {
        Vertex newVertex = new Vertex(type);
        vertices.add(newVertex);
        return newVertex;
    }

    public void addEdge(Vertex v1, Vertex v2, String type, double weight) {
        Edge newEdge = new Edge(v1, v2, type, weight);
        v1.edges.add(newEdge);
    }

    public Vertex getVertex(int counter) {
        for (Vertex vertex : vertices) {
            if (vertex.counter == counter) {
                return vertex;
            }
        }
        return null;
    }


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

    public void fromFile(String filename) {
        vertices.clear();

        try {
            java.util.List<String> lines = Files.readAllLines(Paths.get(filename));
            HashMap<Integer, Vertex> vertexMap = new HashMap<>();

            // First pass: create all vertices from both edge lines and isolated vertex lines
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;

                // Check for vertex format: "(counter imagename)"
                if (line.startsWith("(") && line.endsWith(")")) {
                    String vertexContent = line.substring(1, line.length() - 1); // Remove parentheses
                    String[] parts = vertexContent.split(" ");
                    if (parts.length >= 2) {
                        int counter = Integer.parseInt(parts[0]);
                        String imageName = parts[1]; // e.g., "miner", "smelter", etc.

                        // Convert imageName to VertexType
                        VertexType type = VertexType.UNDEFINED;
                        try {
                            type = VertexType.valueOf(imageName.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            type = VertexType.UNDEFINED;
                        }

                        Vertex vertex = new Vertex(type.toString().toLowerCase());
                        vertex.counter = counter; // Set the correct counter value
                        // Adjust globalCounter to avoid conflicts
                        if (counter >= Vertex.globalCounter) {
                            Vertex.globalCounter = counter + 1;
                        }
                        vertexMap.put(counter, vertex);
                    }
                }
            }

            // Second pass: create edges
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;

                // Check for edge format: "imagename,weight:(counter imagename)->(counter imagename)"
                if (line.contains(":") && line.contains("->")) {
                    String[] parts = line.split(":");
                    if (parts.length >= 2) {
                        String edgeInfo = parts[0]; // e.g., "iron,5.5"
                        String vertexInfo = parts[1]; // e.g., "(2 miner)->(4 smelter)"

                        // Parse edge info: "imagename,weight"
                        String[] edgeParts = edgeInfo.split(",");
                        if (edgeParts.length >= 2) {
                            String imageName = edgeParts[0].trim(); // e.g., "iron"
                            double weight = Double.parseDouble(edgeParts[1].trim());

                            // Parse vertex info: "(counter imagename)->(counter imagename)"
                            String[] vertexParts = vertexInfo.split("->");
                            if (vertexParts.length == 2) {
                                // Parse source vertex: "(counter imagename)"
                                String v1Part = vertexParts[0].trim(); // e.g., "(2 miner)"
                                // Parse destination vertex: "(counter imagename)"
                                String v2Part = vertexParts[1].trim(); // e.g., "(4 smelter)"

                                // Extract counter and imageName from "(counter imagename)" format
                                if (v1Part.startsWith("(") && v1Part.endsWith(")") &&
                                    v2Part.startsWith("(") && v2Part.endsWith(")")) {
                                    String v1Content = v1Part.substring(1, v1Part.length() - 1); // Remove parentheses
                                    String v2Content = v2Part.substring(1, v2Part.length() - 1); // Remove parentheses

                                    String[] v1Parts = v1Content.split(" ");
                                    String[] v2Parts = v2Content.split(" ");

                                    if (v1Parts.length >= 2 && v2Parts.length >= 2) {
                                        int v1Counter = Integer.parseInt(v1Parts[0]);
                                        String v1ImageName = v1Parts[1];
                                        int v2Counter = Integer.parseInt(v2Parts[0]);
                                        String v2ImageName = v2Parts[1];

                                        // Get or create vertices
                                        Vertex v1 = vertexMap.get(v1Counter);
                                        if (v1 == null) {
                                            // Create vertex with the correct type from imageName
                                            VertexType v1Type = VertexType.UNDEFINED;
                                            try {
                                                v1Type = VertexType.valueOf(v1ImageName.toUpperCase());
                                            } catch (IllegalArgumentException e) {
                                                v1Type = VertexType.UNDEFINED;
                                            }
                                            v1 = new Vertex(v1Type.toString().toLowerCase());
                                            v1.counter = v1Counter;
                                            if (v1Counter >= Vertex.globalCounter) {
                                                Vertex.globalCounter = v1Counter + 1;
                                            }
                                            vertexMap.put(v1Counter, v1);
                                        }

                                        Vertex v2 = vertexMap.get(v2Counter);
                                        if (v2 == null) {
                                            // Create vertex with the correct type from imageName
                                            VertexType v2Type = VertexType.UNDEFINED;
                                            try {
                                                v2Type = VertexType.valueOf(v2ImageName.toUpperCase());
                                            } catch (IllegalArgumentException e) {
                                                v2Type = VertexType.UNDEFINED;
                                            }
                                            v2 = new Vertex(v2Type.toString().toLowerCase());
                                            v2.counter = v2Counter;
                                            if (v2Counter >= Vertex.globalCounter) {
                                                Vertex.globalCounter = v2Counter + 1;
                                            }
                                            vertexMap.put(v2Counter, v2);
                                        }

                                        // Add the edge
                                        if (v1 != null && v2 != null) {
                                            addEdge(v1, v2, imageName, weight);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Add all vertices to the graph's vertex list (in case any were missed)
            vertices.addAll(vertexMap.values());
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public Vertex fromString(String str){

        if (str.contains("):")) {
            String vertexPart = str.substring(0, str.indexOf("):") + 2);
            String vertexContent = vertexPart.substring(1, vertexPart.length() - 2); // Remove parentheses
            String[] parts = vertexContent.split(" ");
            if (parts.length >= 2) {
                int counter = Integer.parseInt(parts[0]);
                String typeStr = parts[1].replace(".png", ""); // Remove .png extension
                try {
                    VertexType type = VertexType.valueOf(typeStr.toUpperCase());
                    Vertex vertex = new Vertex(type.toString().toLowerCase());
                    vertex.counter = counter; // Set the correct counter value
 
                    if (counter >= Vertex.globalCounter) {
                        Vertex.globalCounter = counter + 1;
                    }
                    return vertex;
                } catch (IllegalArgumentException e) {
                    return new Vertex("undefined");
                }
            }
        } else if (str.contains("->")) {

            String[] parts = str.split(":");
            if (parts.length >= 2) {
                String edgeInfo = parts[0]; // e.g., "iron.png, 5.5"
                String vertexInfo = parts[1]; // e.g., "1->2"


                String[] edgeParts = edgeInfo.split(", ");
                String typeStr = edgeParts[0].replace(".png", ""); // Remove .png
                double weight = Double.parseDouble(edgeParts[1]);

                String[] vertexParts = vertexInfo.split("->");
                int v1Counter = Integer.parseInt(vertexParts[0]);
                int v2Counter = Integer.parseInt(vertexParts[1]);

                return null;
            }
        }

        return null;
    }
}