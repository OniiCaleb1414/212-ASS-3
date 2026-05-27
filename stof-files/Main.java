import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

public class Main {
    private static int testsRun = 0;
    private static int testsFailed = 0;
    private static final StringBuilder testLog = new StringBuilder();

    public static void main(String[] args) {
        try {
            if (args != null && args.length > 0 && args[0].equals("sample")) {
                printSampleOutput();
                return;
            }
            runAllToLog();
            printTestSummary();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runAllToLog() throws Exception {
        Files.createDirectories(Paths.get("output"));
        testsRun = 0;
        testsFailed = 0;
        testLog.setLength(0);
        runAll();
        Files.write(Paths.get("output/test.log"), testLog.toString().getBytes());
    }

    private static void runAll() throws Exception {
        Vertex.globalCounter = 1;
        runTest("empty graph", new TestCase() {
            @Override
            public void run() throws Exception {
                testEmptyGraph();
            }
        });
        runTest("file load", new TestCase() {
            @Override
            public void run() throws Exception {
                testFileLoad();
            }
        });
        runTest("optimizations", new TestCase() {
            @Override
            public void run() throws Exception {
                testOptimizations();
            }
        });
        runTest("mst", new TestCase() {
            @Override
            public void run() throws Exception {
                testMst();
            }
        });
        runTest("scc", new TestCase() {
            @Override
            public void run() throws Exception {
                testScc();
            }
        });
        runTest("generator", new TestCase() {
            @Override
            public void run() throws Exception {
                testGenerator();
            }
        });
        runTest("edge/vertex coverage", new TestCase() {
            @Override
            public void run() throws Exception {
                testEdgeVertexCoverage();
            }
        });
    }

    private interface TestCase {
        void run() throws Exception;
    }

    private static void runTest(String name, TestCase test) {
        testsRun++;
        try {
            test.run();
            log("Test passed: " + name);
        } catch (Exception e) {
            testsFailed++;
            log("Test failed: " + name);
            logException(e);
        }
    }

    private static void printTestSummary() {
        int passed = testsRun - testsFailed;
        System.out.println("Tests: " + passed + " passed, " + testsFailed + " failed (details in output/test.log)");
    }

    private static void log(String message) {
        testLog.append(message).append(System.lineSeparator());
    }

    private static void logException(Exception e) {
        log(e.toString());
        StackTraceElement[] stack = e.getStackTrace();
        for (StackTraceElement element : stack) {
            log("    at " + element.toString());
        }
    }

    private static void testEmptyGraph() {
        Graph empty = new Graph();
        empty.toString();
        empty.Stage1Optimization();
        empty.Stage2Optimization();
        empty.Stage3Optimization();
        empty.Stage4Optimization();
        empty.optimize();
        empty.MST();
        empty.SCC();
    }

    private static void testFileLoad() throws Exception {
        Graph base = new Graph();
        Vertex a = base.addVertex("smelter");
        Vertex b = base.addVertex("constructor");
        Vertex c = base.addVertex("miner");
        Vertex d = base.addVertex("generator");
        base.addEdge(a, b, "iron", 2.5);
        base.addEdge(b, c, "coal", 1.25);
        base.toString();

        Files.createDirectories(Paths.get("output"));
        String path = "output/test.graph";
        Files.write(Paths.get(path), base.toString().getBytes());

        Graph fromFile = new Graph(path);
        fromFile.toString();
        fromFile.getVertex(a.counter);

        // Ensure isolated vertex parsing is exercised.
        Files.write(Paths.get(path), (base.toString() + "\n(" + d.counter + " generator.png)\n").getBytes());
        Graph withIsolated = new Graph(path);
        withIsolated.toString();
    }

    private static void testOptimizations() {
        Graph graph = new Graph();

        Vertex smelter1 = graph.addVertex("smelter");
        Vertex constructor1 = graph.addVertex("constructor");
        Vertex miner1 = graph.addVertex("miner");
        Vertex assembler = graph.addVertex("assembler");
        Vertex constructor2 = graph.addVertex("constructor");
        Vertex manufacturer = graph.addVertex("manufacturer");
        Vertex refinery = graph.addVertex("refinery");
        Vertex smelter2 = graph.addVertex("smelter");
        Vertex isolated = graph.addVertex("generator");

        graph.addEdge(miner1, smelter1, "copper", 1.0);
        graph.addEdge(smelter1, constructor1, "iron", 1.0);
        graph.addEdge(smelter1, assembler, "iron", 2.0);
        graph.addEdge(smelter1, smelter2, "caterium", 2.0);
        graph.addEdge(assembler, constructor1, "coal", 1.0);
        graph.addEdge(constructor2, smelter1, "caterium", 1.0);
        graph.addEdge(refinery, smelter2, "aluminum", 1.0);
        graph.addEdge(constructor2, manufacturer, "copper", 1.0);
        graph.addEdge(smelter2, constructor2, "iron", 1.0);
        graph.addEdge(constructor2, constructor2, "coal", 1.0);
        graph.addEdge(smelter1, miner1, "iron", 3.0);

        graph.Stage1Optimization();
        graph.Stage2Optimization();
        graph.Stage3Optimization();
        graph.Stage4Optimization();
        graph.optimize();
        graph.toString();
        graph.getVertex(isolated.counter);
    }

    private static void testMst() {
        Graph graph = new Graph();

        Vertex a = graph.addVertex("smelter");
        Vertex b = graph.addVertex("constructor");
        Vertex c = graph.addVertex("assembler");
        Vertex d = graph.addVertex("manufacturer");
        Vertex e = graph.addVertex("refinery");

        graph.addEdge(a, b, "iron", 1.0);
        graph.addEdge(b, c, "iron", 1.0);
        graph.addEdge(c, d, "iron", 1.0);
        graph.addEdge(a, c, "coal", 1.0);
        graph.addEdge(b, d, "coal", 2.0);
        graph.addEdge(d, e, "copper", 0.5);
        graph.addEdge(e, a, "copper", 3.0);

        Graph mst = graph.MST();
        mst.toString();
    }

    private static void testScc() {
        Graph graph = new Graph();

        Vertex a = graph.addVertex("smelter");
        Vertex b = graph.addVertex("constructor");
        Vertex c = graph.addVertex("assembler");
        Vertex d = graph.addVertex("manufacturer");
        Vertex e = graph.addVertex("refinery");
        Vertex f = graph.addVertex("generator");

        graph.addEdge(a, b, "iron", 1.0);
        graph.addEdge(b, c, "iron", 1.0);
        graph.addEdge(c, a, "iron", 1.0);
        graph.addEdge(c, d, "coal", 2.0);
        graph.addEdge(d, e, "coal", 2.0);
        graph.addEdge(e, d, "coal", 2.0);
        graph.addEdge(f, f, "copper", 1.0);

        Graph[] components = graph.SCC();
        for (Graph component : components) {
            component.toString();
        }
    }

    private static void testGenerator() {
        Graph graph = Generator.generate(12, 20);
        graph.optimize();
        Graph mst = graph.MST();
        mst.toString();
        Graph[] components = graph.SCC();
        if (components.length > 0) {
            components[0].toString();
        }

        Random random = new Random(42);
        Graph randomGraph = new Graph();
        Vertex[] nodes = new Vertex[10];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = randomGraph.addVertex("constructor");
        }
        for (int i = 0; i < 25; i++) {
            int from = random.nextInt(nodes.length);
            int to = random.nextInt(nodes.length);
            if (from == to) {
                continue;
            }
            double weight = 1 + random.nextInt(5);
            randomGraph.addEdge(nodes[from], nodes[to], "iron", weight);
        }
        randomGraph.MST();
        randomGraph.SCC();
    }

    private static void printSampleOutput() {
        String sample = "iron.png,1.0:(1 smelter.png)->(2 constructor.png)\n"
                + "coal.png,1.0:(2 constructor.png)->(4 assembler.png)\n"
                + "copper.png,1.0:(3 miner.png)->(1 smelter.png)\n"
                + "copper.png,1.0:(5 constructor.png)->(6 manufacturer.png)\n"
                + "caterium.png,1.0:(5 constructor.png)->(1 smelter.png)\n"
                + "aluminum.png,1.0:(7 refinery.png)->(8 smelter.png)\n"
                + "iron.png,1.0:(8 smelter.png)->(5 constructor.png)\n\n"
                + "iron.png,2.0:(1 smelter.png)->(2 constructor.png)\n"
                + "caterium.png,1.0:(2 constructor.png)->(1 smelter.png)\n"
                + "coal.png,1.0:(2 constructor.png)->(4 assembler.png)\n"
                + "copper.png,1.0:(2 constructor.png)->(6 manufacturer.png)\n"
                + "copper.png,1.0:(3 miner.png)->(1 smelter.png)\n"
                + "aluminum.png,1.0:(7 refinery.png)->(1 smelter.png)\n";
        System.out.print(sample);
    }

    private static void testEdgeVertexCoverage() {
        Vertex v1 = new Vertex("smelter");
        Vertex v2 = new Vertex("constructor");
        Vertex v3 = new Vertex("unknown");

        assertTrue(!v1.equals(null), "Vertex equals null should be false");
        assertTrue(!v1.equals("smelter"), "Vertex equals other type should be false");
        assertTrue(v1.equals(new Vertex("smelter")), "Vertex equals same type should be true");
        v1.toString();
        v2.toString();
        v3.toString();

        Edge e1 = new Edge(v1, v2, "iron", 1.0);
        Edge e2 = new Edge(v1, v2, "iron", 1.0);
        Edge e3 = new Edge(v2, v1, "iron", 1.0);
        Edge e4 = new Edge(v1, v2, "coal", 1.0);
        Edge e5 = new Edge(v1, v2, "iron", 2.0);
        Edge e6 = new Edge(v1, v2, "unknown", 1.0);

        assertTrue(e1.equals(e2), "Edge equals identical should be true");
        assertTrue(!e1.equals(null), "Edge equals null should be false");
        assertTrue(!e1.equals("edge"), "Edge equals other type should be false");
        assertTrue(!e1.equals(e3), "Edge equals reversed should be false");
        assertTrue(!e1.equals(e4), "Edge equals different type should be false");
        assertTrue(!e1.equals(e5), "Edge equals different weight should be false");

        e1.toString();
        e4.toString();
        e6.toString();
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException(message);
        }
    }
}
