import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

public class Graph {
    private final ArrayList<Vertex> vertices;

    public Graph(String filename) {
        vertices = new ArrayList<>();
        if (filename == null) {
            return;
        }

        String text;
        try {
            text = new String(Files.readAllBytes(Paths.get(filename)));
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return;
        }

        text = text.trim();
        if (text.isEmpty() || text.equals("Empty graph")) {
            return;
        }

        HashMap<Integer, Vertex> map = new HashMap<>();
        int maxCounter = 0;
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!trimmed.contains("->")) {
                Vertex v = parseVertex(trimmed, map);
                if (v != null && v.counter > maxCounter) {
                    maxCounter = v.counter;
                }
                continue;
            }

            int colonIndex = trimmed.indexOf(":");
            if (colonIndex < 0) {
                continue;
            }
            String edgePart = trimmed.substring(0, colonIndex).trim();
            String vertexPart = trimmed.substring(colonIndex + 1).trim();

            String[] edgeTokens = edgePart.split(",");
            if (edgeTokens.length != 2) {
                continue;
            }
            String edgeType = edgeTokens[0].trim();
            double weight = Double.parseDouble(edgeTokens[1].trim());

            String[] vertexTokens = vertexPart.split("->");
            if (vertexTokens.length != 2) {
                continue;
            }
            Vertex v1 = parseVertex(vertexTokens[0].trim(), map);
            Vertex v2 = parseVertex(vertexTokens[1].trim(), map);
            if (v1 != null && v2 != null) {
                v1.edges.add(new Edge(v1, v2, edgeType, weight));
                if (v1.counter > maxCounter) {
                    maxCounter = v1.counter;
                }
                if (v2.counter > maxCounter) {
                    maxCounter = v2.counter;
                }
            }
        }

        for (int i = 1; i <= maxCounter; i++) {
            Vertex v = map.get(i);
            if (v != null) {
                vertices.add(v);
            }
        }
        Vertex.globalCounter = maxCounter + 1;
    }

    public Graph() {
        vertices = new ArrayList<>();
    }

    public void Stage1Optimization() {
        // Remove all edges pointing TO a miner
        for (Vertex vertex : vertices) {
            ArrayList<Edge> filtered = new ArrayList<>();
            for (Edge edge : vertex.edges) {
                if (edge.v2.type != VertexType.MINER) {
                    filtered.add(edge);
                }
            }
            vertex.edges = filtered;
        }

        // Keep only the single highest-weight outgoing edge per vertex
        for (Vertex vertex : vertices) {
            if (vertex.edges.size() <= 1) {
                continue;
            }
            Edge best = vertex.edges.get(0);
            for (int i = 1; i < vertex.edges.size(); i++) {
                Edge edge = vertex.edges.get(i);
                if (edge.weight > best.weight) {
                    best = edge;
                }
            }
            ArrayList<Edge> onlyBest = new ArrayList<>();
            onlyBest.add(best);
            vertex.edges = onlyBest;
        }
    }

    public void Stage2Optimization() {
        HashMap<String, Edge> representatives = new HashMap<>();
        for (Vertex vertex : vertices) {
            if (vertex.edges.isEmpty()) {
                continue;
            }
            ArrayList<Edge> kept = new ArrayList<>();
            for (Edge edge : vertex.edges) {
                String key = edge.v1.type + "|" + edge.v2.type + "|" + edge.type;
                Edge existing = representatives.get(key);
                if (existing == null) {
                    representatives.put(key, edge);
                    kept.add(edge);
                } else {
                    existing.weight += edge.weight;
                }
            }
            vertex.edges = kept;
        }
    }

    public void Stage3Optimization() {
        // Remove self-loops
        for (Vertex vertex : vertices) {
            ArrayList<Edge> filtered = new ArrayList<>();
            for (Edge edge : vertex.edges) {
                if (edge.v1.counter != edge.v2.counter) {
                    filtered.add(edge);
                }
            }
            vertex.edges = filtered;
        }
    }

    public void Stage4Optimization() {
        // Remove vertices that have no edges (incoming or outgoing)
        HashSet<Integer> active = new HashSet<>();
        for (Vertex vertex : vertices) {
            for (Edge edge : vertex.edges) {
                active.add(edge.v1.counter);
                active.add(edge.v2.counter);
            }
        }

        for (int i = vertices.size() - 1; i >= 0; i--) {
            if (!active.contains(vertices.get(i).counter)) {
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
        HashMap<Integer, Vertex> map = new HashMap<>();
        for (Vertex vertex : vertices) {
            Vertex copy = mst.addVertex(vertex.type.toString().toLowerCase());
            copy.counter = vertex.counter;
            map.put(vertex.counter, copy);
        }

        if (vertices.isEmpty()) {
            return mst;
        }

        // Build edge list: each original edge + its reverse (to treat graph as undirected)
        ArrayList<Edge> allEdges = new ArrayList<>();
        HashMap<Edge, Edge> reverseToOriginal = new HashMap<>();
        for (Vertex vertex : vertices) {
            for (Edge edge : vertex.edges) {
                allEdges.add(edge);
                Edge reverse = new Edge(edge.v2, edge.v1,
                        edge.type.toString().toLowerCase(), edge.weight);
                allEdges.add(reverse);
                reverseToOriginal.put(reverse, edge);
            }
        }

        Edge[] array = new Edge[allEdges.size()];
        for (int i = 0; i < allEdges.size(); i++) {
            array[i] = allEdges.get(i);
        }
        // Sort by (weight ASC, v1.counter ASC, v2.counter ASC) so the heap
        // pops ties in the correct order required by the spec.
        sortEdgeArray(array);
        MinHeap heap = new OrderedMinHeap(array);

        int maxCounter = getMaxCounter();
        int[] parent = new int[maxCounter + 1];
        int[] rank = new int[maxCounter + 1];
        for (int i = 1; i <= maxCounter; i++) {
            parent[i] = i;
            rank[i] = 0;
        }

        int targetEdges = Math.max(0, vertices.size() - 1);
        int addedEdges = 0;

        while (heap.size > 0 && addedEdges < targetEdges) {
            Edge edge = heap.pop();

            int a = edge.v1.counter;
            int b = edge.v2.counter;
            if (a <= 0 || b <= 0 || a >= parent.length || b >= parent.length) {
                continue;
            }

            if (findSet(parent, a) != findSet(parent, b)) {
                unionSet(parent, rank, a, b);

                Edge original = reverseToOriginal.get(edge);
                if (original == null) {
                    original = edge;
                }
                Vertex mv1 = map.get(original.v1.counter);
                Vertex mv2 = map.get(original.v2.counter);
                if (mv1 != null && mv2 != null) {
                    mst.addEdge(mv1, mv2,
                            original.type.toString().toLowerCase(), original.weight);
                    addedEdges++;
                }
            }
        }

        return mst;
    }

    public Graph[] SCC() {
        if (vertices.isEmpty()) {
            return new Graph[0];
        }

        int maxCounter = getMaxCounter();
        boolean[] visited = new boolean[maxCounter + 1];
        Stack<Vertex> stack = new Stack<>();

        // Pass 1: iterative post-order DFS on the original graph
        for (Vertex vertex : vertices) {
            if (!visited[vertex.counter]) {
                dfsOrder(vertex, visited, stack);
            }
        }

        // Build transpose (reverse) adjacency list
        ArrayList<Integer>[] reverse = buildReverseAdjacency(maxCounter);

        // Reset visited for pass 2
        for (int i = 0; i <= maxCounter; i++) {
            visited[i] = false;
        }

        // Pass 2: iterative DFS on the transpose in reverse-finish order
        ArrayList<Graph> components = new ArrayList<>();
        while (!stack.isEmpty()) {
            Vertex vertex = stack.pop();
            if (visited[vertex.counter]) {
                continue;
            }
            ArrayList<Integer> component = new ArrayList<>();
            dfsReverse(vertex.counter, visited, reverse, component);
            components.add(buildComponentGraph(component));
        }

        Graph[] result = new Graph[components.size()];
        for (int i = 0; i < components.size(); i++) {
            result[i] = components.get(i);
        }
        return result;
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

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Parse a vertex token of the form "(N type)" or "N type" from a file line.
     * Saves/restores globalCounter so file-loaded vertices don't burn counter slots.
     */
    private Vertex parseVertex(String token, HashMap<Integer, Vertex> map) {
        String cleaned = token.trim();
        if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        int spaceIndex = cleaned.indexOf(' ');
        if (spaceIndex < 0) {
            return null;
        }
        int counter = Integer.parseInt(cleaned.substring(0, spaceIndex).trim());
        String type = cleaned.substring(spaceIndex + 1).trim();

        Vertex vertex = map.get(counter);
        if (vertex == null) {
            // Temporarily bypass the auto-increment so file-loaded vertices
            // don't shift the global counter out of sync.
            int saved = Vertex.globalCounter;
            vertex = new Vertex(type);
            Vertex.globalCounter = saved;
            vertex.counter = counter;
            map.put(counter, vertex);
        }
        return vertex;
    }

    /** Find an existing edge to v2 with the given type in the edge list. */
    private Edge findEdge(ArrayList<Edge> edges, Vertex v2, EdgeType type) {
        for (Edge edge : edges) {
            if (edge.v2 == v2 && edge.type == type) {
                return edge;
            }
        }
        return null;
    }

    /** Sort an edge list by v2.counter ascending (insertion sort — list is small). */
    private void sortEdgesByV2Counter(ArrayList<Edge> edges) {
        for (int i = 1; i < edges.size(); i++) {
            Edge key = edges.get(i);
            int j = i - 1;
            while (j >= 0 && edges.get(j).v2.counter > key.v2.counter) {
                edges.set(j + 1, edges.get(j));
                j--;
            }
            edges.set(j + 1, key);
        }
    }

    private int getMaxCounter() {
        int max = 0;
        for (Vertex vertex : vertices) {
            if (vertex.counter > max) {
                max = vertex.counter;
            }
        }
        return max;
    }

    private int findSet(int[] parent, int value) {
        int root = value;
        while (parent[root] != root) {
            root = parent[root];
        }
        // Path compression
        while (parent[value] != value) {
            int next = parent[value];
            parent[value] = root;
            value = next;
        }
        return root;
    }

    private void unionSet(int[] parent, int[] rank, int a, int b) {
        int rootA = findSet(parent, a);
        int rootB = findSet(parent, b);
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

    private static class OrderedMinHeap extends MinHeap {
        OrderedMinHeap(Edge[] array) {
            super(array);
        }

        @Override
        protected boolean compare(Edge child, Edge parent) {
            int cmp = Double.compare(child.weight, parent.weight);
            if (cmp != 0) {
                return cmp < 0;
            }
            cmp = Integer.compare(child.v1.counter, parent.v1.counter);
            if (cmp != 0) {
                return cmp < 0;
            }
            return Integer.compare(child.v2.counter, parent.v2.counter) < 0;
        }
    }

    /**
     * Iterative post-order DFS (Kosaraju pass 1).
     * Avoids StackOverflowError on large / deeply-chained graphs.
     */
    private void dfsOrder(Vertex start, boolean[] visited, Stack<Vertex> finishStack) {
        Stack<int[]> callStack = new Stack<>();
        visited[start.counter] = true;
        callStack.push(new int[] { start.counter, 0 });

        while (!callStack.isEmpty()) {
            int[] frame = callStack.peek();
            Vertex v = getVertex(frame[0]);
            if (v == null) {
                callStack.pop();
                continue;
            }

            boolean pushed = false;
            while (frame[1] < v.edges.size()) {
                Edge e = v.edges.get(frame[1]++);
                if (e.v2.counter < visited.length && !visited[e.v2.counter]) {
                    visited[e.v2.counter] = true;
                    callStack.push(new int[] { e.v2.counter, 0 });
                    pushed = true;
                    break;
                }
            }

            if (!pushed) {
                finishStack.push(v);
                callStack.pop();
            }
        }
    }

    private ArrayList<Integer>[] buildReverseAdjacency(int maxCounter) {
        @SuppressWarnings("unchecked")
        ArrayList<Integer>[] reverse = new ArrayList[maxCounter + 1];
        for (Vertex vertex : vertices) {
            for (Edge edge : vertex.edges) {
                int to = edge.v2.counter;
                int from = edge.v1.counter;
                if (to < 0 || to > maxCounter) {
                    continue;
                }
                if (reverse[to] == null) {
                    reverse[to] = new ArrayList<>();
                }
                reverse[to].add(from);
            }
        }
        return reverse;
    }

    /**
     * Iterative DFS on the transposed graph (Kosaraju pass 2).
     * Avoids StackOverflowError on large graphs.
     */
    private void dfsReverse(
            int startCounter,
            boolean[] visited,
            ArrayList<Integer>[] reverse,
            ArrayList<Integer> component) {
        Stack<int[]> stack = new Stack<>();
        visited[startCounter] = true;
        component.add(startCounter);
        stack.push(new int[] { startCounter, 0 });

        while (!stack.isEmpty()) {
            int[] frame = stack.peek();
            int counter = frame[0];
            ArrayList<Integer> neighbors = reverse[counter];
            if (neighbors == null || frame[1] >= neighbors.size()) {
                stack.pop();
                continue;
            }
            int next = neighbors.get(frame[1]++);
            if (next >= 0 && next < visited.length && !visited[next]) {
                visited[next] = true;
                component.add(next);
                stack.push(new int[] { next, 0 });
            }
        }
    }

    private Graph buildComponentGraph(ArrayList<Integer> component) {
        Graph graph = new Graph();
        if (component.isEmpty()) {
            return graph;
        }

        int maxCounter = getMaxCounter();
        boolean[] inComponent = new boolean[maxCounter + 1];
        for (int counter : component) {
            if (counter >= 0 && counter < inComponent.length) {
                inComponent[counter] = true;
            }
        }

        HashMap<Integer, Vertex> map = new HashMap<>();
        for (int counter : component) {
            Vertex original = getVertex(counter);
            if (original == null) {
                continue;
            }
            Vertex copy = graph.addVertex(original.type.toString().toLowerCase());
            copy.counter = original.counter;
            map.put(counter, copy);
        }

        for (int counter : component) {
            Vertex original = getVertex(counter);
            Vertex copy = map.get(counter);
            if (original == null || copy == null) {
                continue;
            }
            for (Edge edge : original.edges) {
                if (edge.v2.counter >= 0
                        && edge.v2.counter < inComponent.length
                        && inComponent[edge.v2.counter]) {
                    Vertex toCopy = map.get(edge.v2.counter);
                    if (toCopy != null) {
                        graph.addEdge(copy, toCopy,
                                edge.type.toString().toLowerCase(), edge.weight);
                    }
                }
            }
        }

        return graph;
    }

    /** Insertion-sort edges by (weight ASC, v1.counter ASC, v2.counter ASC). */
    private void sortEdgeArray(Edge[] arr) {
        for (int i = 1; i < arr.length; i++) {
            Edge key = arr[i];
            int j = i - 1;
            while (j >= 0 && compareEdges(arr[j], key) > 0) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = key;
        }
    }

    private int compareEdges(Edge a, Edge b) {
        int cmp = Double.compare(a.weight, b.weight);
        if (cmp != 0) return cmp;
        cmp = Integer.compare(a.v1.counter, b.v1.counter);
        if (cmp != 0) return cmp;
        return Integer.compare(a.v2.counter, b.v2.counter);
    }

    /** Insertion-sort an int list ascending. */
    private void sortIntList(ArrayList<Integer> list) {
        for (int i = 1; i < list.size(); i++) {
            int key = list.get(i);
            int j = i - 1;
            while (j >= 0 && list.get(j) > key) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, key);
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
