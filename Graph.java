
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Stack;

public class Graph {
    private final ArrayList<Vertex> vertices;

    public Graph(String filename) {
        vertices = new ArrayList<>();
        if (filename == null) {
            return;
        }
        HashMap<Integer, Vertex> byCounter = new HashMap<>();
        ArrayList<Integer> counters = new ArrayList<>();
        int maxCounter = 0;
        try {
            for (String line : Files.readAllLines(Paths.get(filename))) {
                if (line == null) {
                    continue;
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.equals("Empty graph")) {
                    return;
                }
                if (trimmed.contains("->")) {
                    String[] parts = trimmed.split(":", 2);
                    if (parts.length != 2) {
                        continue;
                    }
                    String[] edgeParts = parts[0].split(",", 2);
                    if (edgeParts.length != 2) {
                        continue;
                    }
                    String edgeType = edgeParts[0];
                    double weight = Double.parseDouble(edgeParts[1]);
                    String[] endpoints = parts[1].split("->", 2);
                    if (endpoints.length != 2) {
                        continue;
                    }
                    VertexInfo info1 = parseVertexInfo(endpoints[0]);
                    VertexInfo info2 = parseVertexInfo(endpoints[1]);
                    if (info1 == null || info2 == null) {
                        continue;
                    }
                    Vertex v1 = getOrCreateVertex(byCounter, counters, info1.counter, info1.type);
                    Vertex v2 = getOrCreateVertex(byCounter, counters, info2.counter, info2.type);
                    maxCounter = Math.max(maxCounter, Math.max(info1.counter, info2.counter));
                    v1.edges.add(new Edge(v1, v2, edgeType, weight));
                } else if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
                    VertexInfo info = parseVertexInfo(trimmed);
                    if (info == null) {
                        continue;
                    }
                    getOrCreateVertex(byCounter, counters, info.counter, info.type);
                    maxCounter = Math.max(maxCounter, info.counter);
                }
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        counters.sort(Comparator.naturalOrder());
        for (int counter : counters) {
            vertices.add(byCounter.get(counter));
        }
        if (maxCounter > 0) {
            Vertex.globalCounter = Math.max(Vertex.globalCounter, maxCounter + 1);
        }
    }

    public Graph() {
        vertices = new ArrayList<>();
    }

    public void Stage1Optimization() {
        for (Vertex vertex : vertices) {
            ArrayList<Edge> filtered = new ArrayList<>();
            for (Edge edge : vertex.edges) {
                if (edge.v2 != null && edge.v2.type != VertexType.MINER) {
                    filtered.add(edge);
                }
            }
            if (filtered.isEmpty()) {
                vertex.edges = filtered;
                continue;
            }
            Edge best = filtered.get(0);
            for (int i = 1; i < filtered.size(); i++) {
                Edge current = filtered.get(i);
                if (current.weight > best.weight) {
                    best = current;
                }
            }
            ArrayList<Edge> keep = new ArrayList<>();
            keep.add(best);
            vertex.edges = keep;
        }
    }

    public void Stage2Optimization() {
        HashMap<VertexType, Vertex> representativeByType = new HashMap<>();
        IdentityHashMap<Vertex, Vertex> representativeMap = new IdentityHashMap<>();
        ArrayList<Vertex> representatives = new ArrayList<>();

        for (Vertex vertex : vertices) {
            Vertex rep = representativeByType.get(vertex.type);
            if (rep == null) {
                representativeByType.put(vertex.type, vertex);
                representatives.add(vertex);
                rep = vertex;
            }
            representativeMap.put(vertex, rep);
        }

        IdentityHashMap<Vertex, ArrayList<Edge>> newEdges = new IdentityHashMap<>();
        for (Vertex rep : representatives) {
            newEdges.put(rep, new ArrayList<>());
        }

        HashMap<String, Edge> mergedEdges = new HashMap<>();
        for (Vertex vertex : vertices) {
            for (Edge edge : vertex.edges) {
                Vertex newV1 = representativeMap.get(edge.v1);
                Vertex newV2 = representativeMap.get(edge.v2);
                if (newV1 == null || newV2 == null) {
                    continue;
                }
                String key = newV1.counter + "-" + newV2.counter + "-" + edge.type;
                Edge existing = mergedEdges.get(key);
                if (existing == null) {
                    Edge created = new Edge(newV1, newV2, edge.type.toString().toLowerCase(), edge.weight);
                    mergedEdges.put(key, created);
                    newEdges.get(newV1).add(created);
                } else {
                    existing.weight += edge.weight;
                }
            }
        }

        vertices.clear();
        vertices.addAll(representatives);
        for (Vertex rep : representatives) {
            rep.edges = newEdges.get(rep);
        }
    }

    public void Stage3Optimization() {
        for (Vertex vertex : vertices) {
            ArrayList<Edge> filtered = new ArrayList<>();
            for (Edge edge : vertex.edges) {
                if (edge.v1 != null && edge.v2 != null && edge.v1.counter != edge.v2.counter) {
                    filtered.add(edge);
                }
            }
            vertex.edges = filtered;
        }
    }

    public void Stage4Optimization() {
        HashSet<Integer> connected = new HashSet<>();
        for (Vertex vertex : vertices) {
            if (!vertex.edges.isEmpty()) {
                connected.add(vertex.counter);
            }
            for (Edge edge : vertex.edges) {
                if (edge.v2 != null) {
                    connected.add(edge.v2.counter);
                }
            }
        }
        for (int i = 0; i < vertices.size(); i++) {
            if (!connected.contains(vertices.get(i).counter)) {
                vertices.remove(i);
                i--;
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
        if (vertices.isEmpty()) {
            return mst;
        }

        int savedCounter = Vertex.globalCounter;
        HashMap<Integer, Vertex> mstVertices = new HashMap<>();
        for (Vertex vertex : vertices) {
            Vertex copy = mst.addVertex(vertex.type.toString().toLowerCase());
            copy.counter = vertex.counter;
            mstVertices.put(vertex.counter, copy);
        }
        Vertex.globalCounter = savedCounter;

        ArrayList<Edge> edges = new ArrayList<>();
        for (Vertex vertex : vertices) {
            for (Edge edge : vertex.edges) {
                edges.add(new Edge(edge.v1, edge.v2, edge.type.toString().toLowerCase(), edge.weight));
                edges.add(new Edge(edge.v2, edge.v1, edge.type.toString().toLowerCase(), edge.weight));
            }
        }

        Edge[] edgeArray = edges.toArray(new Edge[0]);
        MinHeap heap = new MinHeap(edgeArray);
        ArrayList<Edge> ordered = new ArrayList<>();
        Edge next;
        while ((next = heap.pop()) != null) {
            ordered.add(next);
        }
        ordered.sort((a, b) -> {
            int cmp = Double.compare(a.weight, b.weight);
            if (cmp != 0) {
                return cmp;
            }
            cmp = Integer.compare(a.v1.counter, b.v1.counter);
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(a.v2.counter, b.v2.counter);
        });

        HashMap<Integer, Integer> index = new HashMap<>();
        for (int i = 0; i < vertices.size(); i++) {
            index.put(vertices.get(i).counter, i);
        }
        int[] parent = new int[vertices.size()];
        int[] rank = new int[vertices.size()];
        for (int i = 0; i < parent.length; i++) {
            parent[i] = i;
        }

        HashSet<String> chosenPairs = new HashSet<>();
        for (Edge edge : ordered) {
            Integer left = index.get(edge.v1.counter);
            Integer right = index.get(edge.v2.counter);
            if (left == null || right == null) {
                continue;
            }
            int rootLeft = find(parent, left);
            int rootRight = find(parent, right);
            if (rootLeft != rootRight) {
                union(parent, rank, rootLeft, rootRight);
                String key = pairKey(edge.v1, edge.v2);
                chosenPairs.add(key);
            }
        }

        for (Vertex vertex : vertices) {
            for (Edge edge : vertex.edges) {
                if (chosenPairs.contains(pairKey(edge.v1, edge.v2))) {
                    Vertex v1 = mstVertices.get(edge.v1.counter);
                    Vertex v2 = mstVertices.get(edge.v2.counter);
                    if (v1 != null && v2 != null) {
                        mst.addEdge(v1, v2, edge.type.toString().toLowerCase(), edge.weight);
                    }
                }
            }
        }

        return mst;
    }

    public Graph[] SCC() {
        ArrayList<Graph> components = new ArrayList<>();
        if (vertices.isEmpty()) {
            return new Graph[0];
        }

        IdentityHashMap<Vertex, Boolean> visited = new IdentityHashMap<>();
        Stack<Vertex> order = new Stack<>();
        for (Vertex vertex : vertices) {
            if (!visited.containsKey(vertex)) {
                dfsOrder(vertex, visited, order);
            }
        }

        IdentityHashMap<Vertex, ArrayList<Vertex>> reverse = new IdentityHashMap<>();
        for (Vertex vertex : vertices) {
            reverse.put(vertex, new ArrayList<>());
        }
        for (Vertex vertex : vertices) {
            for (Edge edge : vertex.edges) {
                ArrayList<Vertex> incoming = reverse.get(edge.v2);
                if (incoming != null) {
                    incoming.add(edge.v1);
                }
            }
        }

        visited.clear();
        int savedCounter = Vertex.globalCounter;
        while (!order.isEmpty()) {
            Vertex vertex = order.pop();
            if (visited.containsKey(vertex)) {
                continue;
            }
            ArrayList<Vertex> componentVertices = new ArrayList<>();
            dfsCollect(vertex, visited, reverse, componentVertices);

            Graph component = new Graph();
            IdentityHashMap<Vertex, Vertex> mapping = new IdentityHashMap<>();
            for (Vertex original : componentVertices) {
                Vertex copy = component.addVertex(original.type.toString().toLowerCase());
                copy.counter = original.counter;
                mapping.put(original, copy);
            }
            Vertex.globalCounter = savedCounter;
            IdentityHashMap<Vertex, Boolean> member = new IdentityHashMap<>();
            for (Vertex original : componentVertices) {
                member.put(original, Boolean.TRUE);
            }
            for (Vertex original : componentVertices) {
                for (Edge edge : original.edges) {
                    if (member.containsKey(edge.v2)) {
                        Vertex v1 = mapping.get(edge.v1);
                        Vertex v2 = mapping.get(edge.v2);
                        if (v1 != null && v2 != null) {
                            component.addEdge(v1, v2, edge.type.toString().toLowerCase(), edge.weight);
                        }
                    }
                }
            }
            components.add(component);
        }
        Vertex.globalCounter = savedCounter;

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

    private static VertexInfo parseVertexInfo(String token) {
        if (token == null) {
            return null;
        }
        String trimmed = token.trim();
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        String[] parts = trimmed.split(" ", 2);
        if (parts.length != 2) {
            return null;
        }
        try {
            int counter = Integer.parseInt(parts[0]);
            return new VertexInfo(counter, parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Vertex getOrCreateVertex(HashMap<Integer, Vertex> byCounter, ArrayList<Integer> counters, int counter, String type) {
        Vertex vertex = byCounter.get(counter);
        if (vertex == null) {
            vertex = new Vertex(type);
            vertex.counter = counter;
            byCounter.put(counter, vertex);
            counters.add(counter);
        }
        return vertex;
    }

    private static int find(int[] parent, int index) {
        if (parent[index] != index) {
            parent[index] = find(parent, parent[index]);
        }
        return parent[index];
    }

    private static void union(int[] parent, int[] rank, int left, int right) {
        if (rank[left] < rank[right]) {
            parent[left] = right;
        } else if (rank[left] > rank[right]) {
            parent[right] = left;
        } else {
            parent[right] = left;
            rank[left]++;
        }
    }

    private static String pairKey(Vertex v1, Vertex v2) {
        int a = v1.counter;
        int b = v2.counter;
        if (a < b) {
            return a + "-" + b;
        }
        return b + "-" + a;
    }

    private static void dfsOrder(Vertex vertex, IdentityHashMap<Vertex, Boolean> visited, Stack<Vertex> order) {
        visited.put(vertex, Boolean.TRUE);
        for (Edge edge : vertex.edges) {
            Vertex next = edge.v2;
            if (!visited.containsKey(next)) {
                dfsOrder(next, visited, order);
            }
        }
        order.push(vertex);
    }

    private static void dfsCollect(Vertex vertex, IdentityHashMap<Vertex, Boolean> visited,
                                   IdentityHashMap<Vertex, ArrayList<Vertex>> reverse,
                                   ArrayList<Vertex> component) {
        visited.put(vertex, Boolean.TRUE);
        component.add(vertex);
        ArrayList<Vertex> incoming = reverse.get(vertex);
        if (incoming == null) {
            return;
        }
        for (Vertex next : incoming) {
            if (!visited.containsKey(next)) {
                dfsCollect(next, visited, reverse, component);
            }
        }
    }

    private static class VertexInfo {
        final int counter;
        final String type;

        VertexInfo(int counter, String type) {
            this.counter = counter;
            this.type = type;
        }
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
