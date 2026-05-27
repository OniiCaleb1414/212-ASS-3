import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

public class Graph {
    private final ArrayList<Vertex> vertices;

    public Graph(String filename) {
     this.vertices = new ArrayList<>();
        try {
            java.util.List<String> fileline = Files.readAllLines(Paths.get(filename));
            for (String currentLine : fileline) {
                currentLine = currentLine.trim();
                if (currentLine.isEmpty()) {
                    continue;
                }
                if (currentLine.contains(":")) {
                    String[] colonParts = currentLine.split(":");
                    String edgeInfo = colonParts[0].trim();
                    String vertexConnection = colonParts[1].trim();

                    String[] vertexPair = vertexConnection.split("->");
                    String sourcevertstr = vertexPair[0].trim();
                    String destinationvertstr = vertexPair[1].trim();

                    int sourceCounter = extractCounter(sourcevertstr);
                    int destinationCounter = extractCounter(destinationvertstr);
                    String sourceType = extractType(sourcevertstr);
                    String destinationType = extractType(destinationvertstr);

                    Vertex sourceVertex = getVertex(sourceCounter);
                    if (sourceVertex == null) {
                        sourceVertex = addVertex(sourceType);
                        if (sourceVertex.counter != sourceCounter) {
                            int savedGlobal = Vertex.globalCounter;
                            Vertex.globalCounter = sourceCounter;
                            sourceVertex.counter = sourceCounter;
                            Vertex.globalCounter = savedGlobal;
                        }
                    }

                    Vertex destinationVertex = getVertex(destinationCounter);
                    if (destinationVertex == null) {
                        destinationVertex = addVertex(destinationType);
                        if (destinationVertex.counter != destinationCounter) {
                            int savedGlobal = Vertex.globalCounter;
                            Vertex.globalCounter = destinationCounter;
                            destinationVertex.counter = destinationCounter;
                            Vertex.globalCounter = savedGlobal;
                        }
                    }

                    String[] edgeParts = edgeInfo.split(",");
                    String edgeTypeString = edgeParts[0].trim();
                    double edgeWeight = Double.parseDouble(edgeParts[1].trim());

                    addEdge(sourceVertex, destinationVertex, edgeTypeString, edgeWeight);
                } else {
                    int vertexCounter = extractCounter(currentLine);
                    String vertexType = extractType(currentLine);

                    Vertex isolatedVertex = addVertex(vertexType);
                    if (isolatedVertex.counter != vertexCounter) {
                        int savedGlobal = Vertex.globalCounter;
                        Vertex.globalCounter = vertexCounter;
                        isolatedVertex.counter = vertexCounter;
                        Vertex.globalCounter = savedGlobal;
                    }
                }
            }
        } catch (Exception parseException) {
            parseException.printStackTrace();
        }
    }

    private int extractCounter(String vertexString) {
        String cleaned = vertexString.replace("(", "").replace(")", "").trim();
        String[] parts = cleaned.split(" ");
        return Integer.parseInt(parts[0]);
    }

    private String extractType(String vertexString) {
        String cleaned = vertexString.replace("(", "").replace(")", "").trim();
        String[] parts = cleaned.split(" ");
        if (parts.length > 1) {
            return parts[1];
        }
        return "undefined";
    }

    public Graph() {
        this.vertices = new ArrayList<>();
    }

    public void Stage1Optimization() {
        HashSet<String> observedTypes = new HashSet<>();
        boolean allTypesUnique = true;
        for (Vertex nodeItem : vertices) {
            if (!observedTypes.add(nodeItem.type.toString())) {
                allTypesUnique = false;
                break;
            }
        }
        if (allTypesUnique && !vertices.isEmpty()) {
            return;
        }

        for (Vertex currentVertex : vertices) {
            if (currentVertex.type == VertexType.MINER) {
                ArrayList<Edge> incomingEdgesToRemove = new ArrayList<>();
                for (Vertex potentialSource : vertices) {
                    for (Edge outgoingEdge : potentialSource.edges) {
                        if (outgoingEdge.v2.equals(currentVertex)) {
                            incomingEdgesToRemove.add(outgoingEdge);
                        }
                    }
                }
                for (Edge edgeToRemove : incomingEdgesToRemove) {
                    edgeToRemove.v1.edges.remove(edgeToRemove);
                }
            }
        }

        for (Vertex currentVertex : vertices) {
            if (currentVertex.edges.isEmpty()) {
                continue;
            }
            Edge heaviestEdge = currentVertex.edges.get(0);
            for (Edge candidateEdge : currentVertex.edges) {
                if (candidateEdge.weight > heaviestEdge.weight) {
                    heaviestEdge = candidateEdge;
                }
            }
            ArrayList<Edge> retainedEdges = new ArrayList<>();
            retainedEdges.add(heaviestEdge);
            currentVertex.edges = retainedEdges;
        }
    }

    public void Stage2Optimization() {
          
        ArrayList<Edge> allEdges = new ArrayList<>();
        for (Vertex vert : vertices) {
            allEdges.addAll(vert.edges);
        }

       
        HashMap<Vertex, Vertex> redirectMap = new HashMap<>();
        ArrayList<Edge> edgesToRemove = new ArrayList<>();

        for (int i = 0; i < allEdges.size(); i++) {
            Edge edgeA = allEdges.get(i);
            if (edgesToRemove.contains(edgeA)) continue;

            for (int j = i + 1; j < allEdges.size(); j++) {
                Edge edgeB = allEdges.get(j);
                if (edgesToRemove.contains(edgeB)) continue;

                boolean sameSourceType = edgeA.v1.type == edgeB.v1.type;
                boolean sameDestType = edgeA.v2.type == edgeB.v2.type;
                boolean sameEdgeType = edgeA.type == edgeB.type;
                boolean differentVertices = !edgeA.v1.equals(edgeB.v1) && !edgeA.v2.equals(edgeB.v2);

                if (sameSourceType && sameDestType && sameEdgeType && differentVertices) {
                    edgeA.weight += edgeB.weight;
                    edgesToRemove.add(edgeB);

                    if (!redirectMap.containsKey(edgeB.v1)) {
                        redirectMap.put(edgeB.v1, edgeA.v1);
                    }
                    if (!redirectMap.containsKey(edgeB.v2)) {
                        redirectMap.put(edgeB.v2, edgeA.v2);
                    }
                }
            }
        }

        if (edgesToRemove.isEmpty()) return;

       
        for (Edge deadEdge : edgesToRemove) {
            deadEdge.v1.edges.remove(deadEdge);
        }

        
        for (Vertex key : new ArrayList<>(redirectMap.keySet())) {
            Vertex value = redirectMap.get(key);
            while (redirectMap.containsKey(value)) {
                redirectMap.put(key, redirectMap.get(value));
                value = redirectMap.get(key);
            }
        }

        
        for (Vertex vert : vertices) {
            for (Edge e : vert.edges) {
                if (redirectMap.containsKey(e.v2)) {
                    e.v2 = redirectMap.get(e.v2);
                }
            }
        }

        
        for (Vertex removed : redirectMap.keySet()) {
            Vertex kept = redirectMap.get(removed);
            ArrayList<Edge> remainingEdges = new ArrayList<>(removed.edges);
            for (Edge e : remainingEdges) {
                e.v1 = kept;
                kept.edges.add(e);
            }
            removed.edges.clear();
        }

        
        for (Vertex vert : vertices) {
            ArrayList<Edge> deduped = new ArrayList<>();
            for (Edge e : vert.edges) {
                boolean merged = false;
                for (Edge existing : deduped) {
                    if (existing.v2.equals(e.v2) && existing.type == e.type) {
                        existing.weight += e.weight;
                        merged = true;
                        break;
                    }
                }
                if (!merged) {
                    deduped.add(e);
                }
            }
            vert.edges = deduped;
        }

        
        for (Vertex vert : vertices) {
            ArrayList<Edge> edgeList = vert.edges;
            for (int i = 0; i < edgeList.size() - 1; i++) {
                for (int j = 0; j < edgeList.size() - i - 1; j++) {
                    if (edgeList.get(j).v2.counter > edgeList.get(j + 1).v2.counter) {
                        Edge temp = edgeList.get(j);
                        edgeList.set(j, edgeList.get(j + 1));
                        edgeList.set(j + 1, temp);
                    }
                }
            }
        }

        
        vertices.removeAll(redirectMap.keySet());
    }


    public void Stage3Optimization() {
        for (Vertex currentVertex : vertices) {
            ArrayList<Edge> selfLoopsToRemove = new ArrayList<>();
            for (Edge outgoingEdge : currentVertex.edges) {
                if (outgoingEdge.v1.equals(outgoingEdge.v2)) {
                    selfLoopsToRemove.add(outgoingEdge);
                }
            }
            for (Edge selfLoop : selfLoopsToRemove) {
                currentVertex.edges.remove(selfLoop);
            }
        }
    }

    public void Stage4Optimization() {
         ArrayList<Vertex> verticesToRemove = new ArrayList<>();
        for (Vertex currentVertex : vertices) {
            boolean hasincoming = false;
            for (Vertex potentialSource : vertices) {
                for (Edge outgoingEdge : potentialSource.edges) {
                    if (outgoingEdge.v2.equals(currentVertex)) {
                        hasincoming = true;
                        break;
                    }
                }
                if (hasincoming) {
                    break;
                }
            }
            boolean hasoutgoing = !currentVertex.edges.isEmpty();
            if (!hasincoming && !hasoutgoing) {
                verticesToRemove.add(currentVertex);
            }
        }
        vertices.removeAll(verticesToRemove);
    }

    public void optimize() {
        Stage1Optimization();
        Stage2Optimization();
        Stage3Optimization();
        Stage4Optimization();
    }

    public Graph MST() {
          Graph minimumSpanningTree = new Graph();
        HashMap<Integer, Vertex> vertexMapping = new HashMap<>();

        for (Vertex originalVertex : vertices) {
            Vertex clonedVertex = minimumSpanningTree.addVertex(originalVertex.type.toString().toLowerCase());
            int savedGlobal = Vertex.globalCounter;
            Vertex.globalCounter = originalVertex.counter;
            clonedVertex.counter = originalVertex.counter;
            Vertex.globalCounter = savedGlobal;
            vertexMapping.put(originalVertex.counter, clonedVertex);
        }

        ArrayList<Edge> uniqueEdges = new ArrayList<>();
        HashMap<String, Edge> bestEdgeForPair = new HashMap<>();

        for (Vertex sourceVertex : vertices) {
            for (Edge currentEdge : sourceVertex.edges) {
                int minCounter = Math.min(currentEdge.v1.counter, currentEdge.v2.counter);
                int maxCounter = Math.max(currentEdge.v1.counter, currentEdge.v2.counter);
                String pairKey = minCounter + "-" + maxCounter + "-" + currentEdge.type.toString().toLowerCase();

                Vertex mstV1 = vertexMapping.get(currentEdge.v1.counter);
                Vertex mstV2 = vertexMapping.get(currentEdge.v2.counter);
                Edge mstEdge = new Edge(mstV1, mstV2,
                    currentEdge.type.toString().toLowerCase(), currentEdge.weight);

                if (!bestEdgeForPair.containsKey(pairKey)) {
                    bestEdgeForPair.put(pairKey, mstEdge);
                } else if (mstEdge.weight < bestEdgeForPair.get(pairKey).weight) {
                    bestEdgeForPair.put(pairKey, mstEdge);
                }
            }
        }
        uniqueEdges.addAll(bestEdgeForPair.values());

        
        for (int outerIdx = 0; outerIdx < uniqueEdges.size() - 1; outerIdx++) {
            for (int innerIdx = outerIdx + 1; innerIdx < uniqueEdges.size(); innerIdx++) {
                if (uniqueEdges.get(outerIdx).weight > uniqueEdges.get(innerIdx).weight) {
                    Edge temp = uniqueEdges.get(outerIdx);
                    uniqueEdges.set(outerIdx, uniqueEdges.get(innerIdx));
                    uniqueEdges.set(innerIdx, temp);
                } else if (Math.abs(uniqueEdges.get(outerIdx).weight - uniqueEdges.get(innerIdx).weight) < 0.0001) {
                    if (uniqueEdges.get(outerIdx).v1.counter > uniqueEdges.get(innerIdx).v1.counter) {
                        Edge temp = uniqueEdges.get(outerIdx);
                        uniqueEdges.set(outerIdx, uniqueEdges.get(innerIdx));
                        uniqueEdges.set(innerIdx, temp);
                    } else if (uniqueEdges.get(outerIdx).v1.counter == uniqueEdges.get(innerIdx).v1.counter) {
                        if (uniqueEdges.get(outerIdx).v2.counter > uniqueEdges.get(innerIdx).v2.counter) {
                            Edge temp = uniqueEdges.get(outerIdx);
                            uniqueEdges.set(outerIdx, uniqueEdges.get(innerIdx));
                            uniqueEdges.set(innerIdx, temp);
                        }
                    }
                }
            }
        }

        HashMap<Integer, Integer> disjointSet = new HashMap<>();
        for (Vertex vertexItem : vertexMapping.values()) {
            disjointSet.put(vertexItem.counter, vertexItem.counter);
        }

        for (Edge smallestEdge : uniqueEdges) {
            if (smallestEdge == null) continue;

            int sourceRepresentative = findSetRepresentative(disjointSet, smallestEdge.v1.counter);
            int destRepresentative = findSetRepresentative(disjointSet, smallestEdge.v2.counter);

            if (sourceRepresentative != destRepresentative) {
                minimumSpanningTree.addEdge(smallestEdge.v1, smallestEdge.v2,
                    smallestEdge.type.toString().toLowerCase(), smallestEdge.weight);
                unionSets(disjointSet, sourceRepresentative, destRepresentative);
            }
        }

        return minimumSpanningTree;
    }

    private int findSetRepresentative(HashMap<Integer, Integer> parentMapping, int vertexId) {
        if (parentMapping.get(vertexId) != vertexId) {
            parentMapping.put(vertexId, findSetRepresentative(parentMapping, parentMapping.get(vertexId)));
        }
        return parentMapping.get(vertexId);
    }

    private void unionSets(HashMap<Integer, Integer> parentMapping, int representativeA, int representativeB) {
        parentMapping.put(representativeB, representativeA);
    }


    public Graph[] SCC() {
        if (vertices.isEmpty()) {
            return new Graph[0];
        }

        int maxcount = 0;
        for (Vertex v : vertices) {
            if (v.counter > maxcount) {
                maxcount = v.counter;
            }
        }

        ArrayList<ArrayList<Integer>> adjacencyList = new ArrayList<>();
        for (int idx = 0; idx <= maxcount; idx++) {
            adjacencyList.add(new ArrayList<Integer>());
        }

        for (Vertex sourceVertex : vertices) {
            for (Edge outgoingEdge : sourceVertex.edges) {
                adjacencyList.get(sourceVertex.counter).add(outgoingEdge.v2.counter);
            }
        }

        int[] discoveryTimes = new int[maxcount + 1];
        int[] lowValues = new int[maxcount + 1];
        boolean[] inStack = new boolean[maxcount + 1];
        boolean[] visited = new boolean[maxcount + 1];
        Stack<Integer> dfsStack = new Stack<>();
        int[] timer = new int[]{0};
        ArrayList<ArrayList<Integer>> allComponents = new ArrayList<>();

        for (Vertex startVertex : vertices) {
            if (!visited[startVertex.counter]) {
                tarjanDFS(startVertex.counter, adjacencyList, discoveryTimes, lowValues, 
                    inStack, dfsStack, timer, allComponents, visited);
            }
        }

        Graph[] result = new Graph[allComponents.size()];
        for (int compIdx = 0; compIdx < allComponents.size(); compIdx++) {
            Graph componentGraph = new Graph();
            ArrayList<Integer> component = allComponents.get(compIdx);

            for (Integer vertexCounter : component) {
                Vertex originalVertex = getVertex(vertexCounter);
                if (originalVertex != null) {
                    Vertex newVertex = componentGraph.addVertex(originalVertex.type.toString().toLowerCase());
                    int savedGlobal = Vertex.globalCounter;
                    Vertex.globalCounter = vertexCounter;
                    newVertex.counter = vertexCounter;
                    Vertex.globalCounter = savedGlobal;
                }
            }

            for (Integer vertexCounter : component) {
                Vertex originalVertex = getVertex(vertexCounter);
                if (originalVertex != null) {
                    for (Edge originalEdge : originalVertex.edges) {
                        if (component.contains(originalEdge.v2.counter)) {
                            Vertex compSource = componentGraph.getVertex(originalEdge.v1.counter);
                            Vertex compDest = componentGraph.getVertex(originalEdge.v2.counter);
                            if (compSource != null && compDest != null) {
                                componentGraph.addEdge(compSource, compDest, 
                                    originalEdge.type.toString().toLowerCase(), originalEdge.weight);
                            }
                        }
                    }
                }
            }
            result[compIdx] = componentGraph;
        }

        return result;
    }

    private void tarjanDFS(int currentCounter, ArrayList<ArrayList<Integer>> adjacencyList,
        int[] discoveryTimes, int[] lowValues, boolean[] inStack, Stack<Integer> dfsStack,
        int[] timer, ArrayList<ArrayList<Integer>> allComponents, boolean[] visited) {

        visited[currentCounter] = true;
        timer[0]++;
        discoveryTimes[currentCounter] = timer[0];
        lowValues[currentCounter] = timer[0];
        dfsStack.push(currentCounter);
        inStack[currentCounter] = true;

        for (Integer neighborCounter : adjacencyList.get(currentCounter)) {
            if (!visited[neighborCounter]) {
                tarjanDFS(neighborCounter, adjacencyList, discoveryTimes, lowValues, 
                    inStack, dfsStack, timer, allComponents, visited);
                lowValues[currentCounter] = Math.min(lowValues[currentCounter], lowValues[neighborCounter]);
            } else if (inStack[neighborCounter]) {
                lowValues[currentCounter] = Math.min(lowValues[currentCounter], discoveryTimes[neighborCounter]);
            }
        }

        if (lowValues[currentCounter] == discoveryTimes[currentCounter]) {
            ArrayList<Integer> newComponent = new ArrayList<>();
            while (true) {
                int poppedVertex = dfsStack.pop();
                inStack[poppedVertex] = false;
                newComponent.add(poppedVertex);
                if (poppedVertex == currentCounter) {
                    break;
                }
            }
            allComponents.add(newComponent);
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
          for (Vertex candidate : vertices) {
            if (candidate.counter == counter) {
                return candidate;
            }
        }
        return null;
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
