
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Stack;

public class Graph {
    private final ArrayList<Vertex> vertices;

    public Graph(String filename) {
        this();
        int maxCounter = 0;
        try {
            for (String line : Files.readAllLines(Paths.get(filename))) {
                if (line == null) {
                    continue;
                }
                line = line.trim();
                if (line.isEmpty() || line.equals("Empty graph")) {
                    continue;
                }
                if (line.contains("->")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length < 2) {
                        continue;
                    }
                    String edgePart = parts[0].trim();
                    String verticesPart = parts[1].trim();
                    String[] edgeParts = edgePart.split(",", 2);
                    if (edgeParts.length < 2) {
                        continue;
                    }
                    String edgeType = normalizeType(edgeParts[0]);
                    double weight;
                    try {
                        weight = Double.parseDouble(edgeParts[1].trim());
                    } catch (NumberFormatException e) {
                        continue;
                    }

                    int arrowIndex = verticesPart.indexOf("->");
                    if (arrowIndex < 0) {
                        continue;
                    }
                    String v1Token = verticesPart.substring(0, arrowIndex).trim();
                    String v2Token = verticesPart.substring(arrowIndex + 2).trim();
                    Vertex v1 = ensureVertex(v1Token);
                    Vertex v2 = ensureVertex(v2Token);
                    if (v1 != null && v1.counter > maxCounter) {
                        maxCounter = v1.counter;
                    }
                    if (v2 != null && v2.counter > maxCounter) {
                        maxCounter = v2.counter;
                    }
                    if (v1 != null && v2 != null) {
                        addEdge(v1, v2, edgeType, weight);
                    }
                } else {
                    Vertex vertex = ensureVertex(line);
                    if (vertex != null && vertex.counter > maxCounter) {
                        maxCounter = vertex.counter;
                    }
                }
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        if (Vertex.globalCounter <= maxCounter) {
            Vertex.globalCounter = maxCounter + 1;
        }
    }

    public Graph() {
        vertices = new ArrayList<>();
    }

    public void Stage1Optimization() {
        for (Vertex vertex : vertices) {
            for (int i = vertex.edges.size() - 1; i >= 0; i--) {
                if (vertex.edges.get(i).v2.type == VertexType.MINER) {
                    vertex.edges.remove(i);
                }
            }
        }

        for (Vertex vertex : vertices) {
            if (vertex.edges.size() <= 1) {
                continue;
            }
            double maxWeight = Double.NEGATIVE_INFINITY;
            int maxIndex = -1;
            for (int i = 0; i < vertex.edges.size(); i++) {
                double weight = vertex.edges.get(i).weight;
                if (weight > maxWeight) {
                    maxWeight = weight;
                    maxIndex = i;
                }
            }
            for (int i = vertex.edges.size() - 1; i >= 0; i--) {
                if (i != maxIndex) {
                    vertex.edges.remove(i);
                }
            }
        }
    }

    public void Stage2Optimization() {
        ArrayList<Edge> allEdges = new ArrayList<>();
        for (Vertex vertex : vertices) {
            allEdges.addAll(vertex.edges);
        }

        HashMap<String, Edge> patternMap = new HashMap<>();
        IdentityHashMap<Edge, Boolean> removedEdges = new IdentityHashMap<>();
        IdentityHashMap<Vertex, Vertex> replacements = new IdentityHashMap<>();

        for (Edge edge : allEdges) {
            String key = edge.v1.type + "|" + edge.v2.type + "|" + edge.type;
            Edge representative = patternMap.get(key);
            if (representative == null) {
                patternMap.put(key, edge);
                continue;
            }
            if (representative == edge) {
                continue;
            }
            representative.weight += edge.weight;
            removedEdges.put(edge, Boolean.TRUE);
            if (edge.v1 != representative.v1 && !replacements.containsKey(edge.v1)) {
                replacements.put(edge.v1, representative.v1);
            }
            if (edge.v2 != representative.v2 && !replacements.containsKey(edge.v2)) {
                replacements.put(edge.v2, representative.v2);
            }
        }

        for (Vertex vertex : vertices) {
            vertex.edges = new ArrayList<>();
        }

        for (Edge edge : allEdges) {
            if (removedEdges.containsKey(edge)) {
                continue;
            }
            Vertex newV1 = resolveVertex(edge.v1, replacements);
            Vertex newV2 = resolveVertex(edge.v2, replacements);
            edge.v1 = newV1;
            edge.v2 = newV2;
            newV1.edges.add(edge);
        }
    }

    public void Stage3Optimization() {
        for (Vertex vertex : vertices) {
            for (int i = vertex.edges.size() - 1; i >= 0; i--) {
                Edge edge = vertex.edges.get(i);
                if (edge.v1 == edge.v2) {
                    vertex.edges.remove(i);
                }
            }
        }
    }

    public void Stage4Optimization() {
        HashSet<Integer> incoming = new HashSet<>();
        for (Vertex vertex : vertices) {
            for (Edge edge : vertex.edges) {
                incoming.add(edge.v2.counter);
            }
        }

        for (int i = vertices.size() - 1; i >= 0; i--) {
            Vertex vertex = vertices.get(i);
            if (vertex.edges.isEmpty() && !incoming.contains(vertex.counter)) {
                vertices.remove(i);
            }
        }
    }

    public void optimize() {
        Stage1Optimization();
        Stage2Optimization();
        Stage3Optimization();
        Stage4Optimization();
    }

    public Graph MST() {
        Graph mst = new Graph();

        IdentityHashMap<Vertex, Vertex> vertexMap = new IdentityHashMap<>();
        int maxCounter = 0;
        for (Vertex vertex : vertices) {
            Vertex copy = new Vertex(vertexTypeToString(vertex.type));
            copy.counter = vertex.counter;
            copy.edges = new ArrayList<>();
            mst.vertices.add(copy);
            vertexMap.put(vertex, copy);
            if (vertex.counter > maxCounter) {
                maxCounter = vertex.counter;
            }
        }

        if (Vertex.globalCounter <= maxCounter) {
            Vertex.globalCounter = maxCounter + 1;
        }

        ArrayList<Edge> originalEdges = new ArrayList<>();
        for (Vertex vertex : vertices) {
            originalEdges.addAll(vertex.edges);
        }

        HashMap<String, ArrayList<Edge>> edgePairs = new HashMap<>();
        for (Edge edge : originalEdges) {
            String key = pairKey(edge.v1.counter, edge.v2.counter);
            ArrayList<Edge> list = edgePairs.get(key);
            if (list == null) {
                list = new ArrayList<>();
                edgePairs.put(key, list);
            }
            list.add(edge);
        }

        ArrayList<Edge> heapEdges = new ArrayList<>();
        for (Edge edge : originalEdges) {
            heapEdges.add(edge);
            heapEdges.add(new Edge(edge.v2, edge.v1, edgeTypeToString(edge.type), edge.weight));
        }

        MinHeap heap = new MinHeap(heapEdges.toArray(new Edge[0]));

        IdentityHashMap<Vertex, Integer> indexMap = new IdentityHashMap<>();
        for (int i = 0; i < vertices.size(); i++) {
            indexMap.put(vertices.get(i), i);
        }

        int[] parent = new int[vertices.size()];
        int[] rank = new int[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            parent[i] = i;
        }

        int unions = 0;
        HashSet<String> addedEdges = new HashSet<>();
        while (heap.size > 0 && unions < vertices.size() - 1) {
            Edge edge = heap.pop();
            if (edge == null) {
                break;
            }
            double weight = edge.weight;
            ArrayList<Edge> sameWeight = new ArrayList<>();
            sameWeight.add(edge);
            while (heap.size > 0 && Double.compare(heap.data.get(0).weight, weight) == 0) {
                sameWeight.add(heap.pop());
            }

            sameWeight.sort((a, b) -> {
                if (a.v1.counter != b.v1.counter) {
                    return Integer.compare(a.v1.counter, b.v1.counter);
                }
                return Integer.compare(a.v2.counter, b.v2.counter);
            });

            for (Edge candidate : sameWeight) {
                Integer i = indexMap.get(candidate.v1);
                Integer j = indexMap.get(candidate.v2);
                if (i == null || j == null) {
                    continue;
                }
                int rootI = find(parent, i);
                int rootJ = find(parent, j);
                if (rootI == rootJ) {
                    continue;
                }
                union(parent, rank, rootI, rootJ);
                unions++;
                String key = pairKey(candidate.v1.counter, candidate.v2.counter);
                ArrayList<Edge> originals = edgePairs.get(key);
                if (originals != null) {
                    for (Edge original : originals) {
                        String edgeKey = edgeKey(original);
                        if (addedEdges.contains(edgeKey)) {
                            continue;
                        }
                        Vertex newV1 = vertexMap.get(original.v1);
                        Vertex newV2 = vertexMap.get(original.v2);
                        if (newV1 != null && newV2 != null) {
                            mst.addEdge(newV1, newV2, edgeTypeToString(original.type), original.weight);
                            addedEdges.add(edgeKey);
                        }
                    }
                }
                if (unions == vertices.size() - 1) {
                    break;
                }
            }
        }

        return mst;
    }

    public Graph[] SCC() {
        if (vertices.isEmpty()) {
            return new Graph[0];
        }

        IdentityHashMap<Vertex, Integer> indexMap = new IdentityHashMap<>();
        for (int i = 0; i < vertices.size(); i++) {
            indexMap.put(vertices.get(i), i);
        }

        boolean[] visited = new boolean[vertices.size()];
        Stack<Vertex> stack = new Stack<>();

        for (Vertex vertex : vertices) {
            int idx = indexMap.get(vertex);
            if (!visited[idx]) {
                fillOrder(vertex, visited, indexMap, stack);
            }
        }

        ArrayList<ArrayList<Vertex>> reverse = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            reverse.add(new ArrayList<>());
        }
        for (Vertex vertex : vertices) {
            for (Edge edge : vertex.edges) {
                int toIndex = indexMap.get(edge.v2);
                reverse.get(toIndex).add(edge.v1);
            }
        }

        boolean[] visitedReverse = new boolean[vertices.size()];
        ArrayList<Graph> components = new ArrayList<>();

        while (!stack.isEmpty()) {
            Vertex vertex = stack.pop();
            int idx = indexMap.get(vertex);
            if (visitedReverse[idx]) {
                continue;
            }
            ArrayList<Vertex> componentVertices = new ArrayList<>();
            dfsReverse(vertex, visitedReverse, reverse, indexMap, componentVertices);
            components.add(buildComponentGraph(componentVertices));
        }

        return components.toArray(new Graph[0]);
    }

    public Vertex addVertex(String type) {
        Vertex vertex = new Vertex(type);
        vertices.add(vertex);
        return vertex;
    }

    public void addEdge(Vertex v1, Vertex v2, String type, double weight) {
        if (v1 == null || v2 == null) {
            return;
        }
        v1.edges.add(new Edge(v1, v2, type, weight));
    }

    public Vertex getVertex(int counter) {
        for (Vertex vertex : vertices) {
            if (vertex.counter == counter) {
                return vertex;
            }
        }
        return null;
    }

    private Vertex ensureVertex(String token) {
        String cleaned = token.trim();
        if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        String[] parts = cleaned.trim().split("\\s+");
        if (parts.length == 0) {
            return null;
        }
        int counter;
        try {
            counter = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return null;
        }
        Vertex existing = getVertex(counter);
        if (existing != null) {
            return existing;
        }
        String type = parts.length > 1 ? normalizeType(parts[1]) : "undefined";
        Vertex vertex = new Vertex(type);
        vertex.counter = counter;
        vertices.add(vertex);
        return vertex;
    }

    private static String normalizeType(String value) {
        if (value == null) {
            return "undefined";
        }
        String cleaned = value.trim();
        if (cleaned.endsWith(".png")) {
            cleaned = cleaned.substring(0, cleaned.length() - 4);
        }
        return cleaned;
    }

    private static String vertexTypeToString(VertexType type) {
        switch (type) {
            case MINER:
                return "miner";
            case SMELTER:
                return "smelter";
            case CONSTRUCTOR:
                return "constructor";
            case ASSEMBLER:
                return "assembler";
            case MANUFACTURER:
                return "manufacturer";
            case REFINERY:
                return "refinery";
            case GENERATOR:
                return "generator";
            default:
                return "undefined";
        }
    }

    private static String edgeTypeToString(EdgeType type) {
        switch (type) {
            case IRON:
                return "iron";
            case COAL:
                return "coal";
            case COPPER:
                return "copper";
            case CATERIUM:
                return "caterium";
            case ALUMINUM:
                return "aluminum";
            default:
                return "undefined";
        }
    }

    private static String pairKey(int a, int b) {
        int min = Math.min(a, b);
        int max = Math.max(a, b);
        return min + ":" + max;
    }

    private static String edgeKey(Edge edge) {
        return edge.v1.counter + "->" + edge.v2.counter + ":" + edge.type + ":" + edge.weight;
    }

    private static Vertex resolveVertex(Vertex vertex, IdentityHashMap<Vertex, Vertex> replacements) {
        Vertex current = vertex;
        while (replacements.containsKey(current)) {
            current = replacements.get(current);
        }
        return current;
    }

    private static int find(int[] parent, int index) {
        if (parent[index] != index) {
            parent[index] = find(parent, parent[index]);
        }
        return parent[index];
    }

    private static void union(int[] parent, int[] rank, int rootA, int rootB) {
        if (rootA == rootB) {
            return;
        }
        if (rank[rootA] < rank[rootB]) {
            parent[rootA] = rootB;
        } else if (rank[rootA] > rank[rootB]) {
            parent[rootB] = rootA;
        } else {
            parent[rootB] = rootA;
            rank[rootA]++;
        }
    }

    private void fillOrder(Vertex vertex, boolean[] visited, IdentityHashMap<Vertex, Integer> indexMap, Stack<Vertex> stack) {
        int idx = indexMap.get(vertex);
        visited[idx] = true;
        for (Edge edge : vertex.edges) {
            Vertex next = edge.v2;
            int nextIdx = indexMap.get(next);
            if (!visited[nextIdx]) {
                fillOrder(next, visited, indexMap, stack);
            }
        }
        stack.push(vertex);
    }

    private void dfsReverse(Vertex vertex, boolean[] visited, ArrayList<ArrayList<Vertex>> reverse,
                            IdentityHashMap<Vertex, Integer> indexMap, ArrayList<Vertex> component) {
        int idx = indexMap.get(vertex);
        visited[idx] = true;
        component.add(vertex);
        for (Vertex next : reverse.get(idx)) {
            int nextIdx = indexMap.get(next);
            if (!visited[nextIdx]) {
                dfsReverse(next, visited, reverse, indexMap, component);
            }
        }
    }

    private Graph buildComponentGraph(ArrayList<Vertex> componentVertices) {
        Graph component = new Graph();

        IdentityHashMap<Vertex, Vertex> map = new IdentityHashMap<>();
        int maxCounter = 0;
        for (Vertex vertex : componentVertices) {
            Vertex copy = new Vertex(vertexTypeToString(vertex.type));
            copy.counter = vertex.counter;
            copy.edges = new ArrayList<>();
            component.vertices.add(copy);
            map.put(vertex, copy);
            if (vertex.counter > maxCounter) {
                maxCounter = vertex.counter;
            }
        }

        if (Vertex.globalCounter <= maxCounter) {
            Vertex.globalCounter = maxCounter + 1;
        }

        for (Vertex vertex : componentVertices) {
            for (Edge edge : vertex.edges) {
                Vertex newV1 = map.get(edge.v1);
                Vertex newV2 = map.get(edge.v2);
                if (newV1 != null && newV2 != null) {
                    component.addEdge(newV1, newV2, edgeTypeToString(edge.type), edge.weight);
                }
            }
        }

        return component;
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
