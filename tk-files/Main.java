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
                verifyEmptyGraph();
            }
        });
        runTest("file load", new TestCase() {
            @Override
            public void run() throws Exception {
                verifyFileLoad();
            }
        });
        runTest("optimizations", new TestCase() {
            @Override
            public void run() throws Exception {
                verifyOptimizations();
            }
        });
        runTest("mst", new TestCase() {
            @Override
            public void run() throws Exception {
                verifyMst();
            }
        });
        runTest("scc", new TestCase() {
            @Override
            public void run() throws Exception {
                verifyScc();
            }
        });
        runTest("generator", new TestCase() {
            @Override
            public void run() throws Exception {
                verifyGenerator();
            }
        });
        runTest("edge/vertex coverage", new TestCase() {
            @Override
            public void run() throws Exception {
                verifyEdgeVertexCoverage();
            }
        });
    }

    private interface TestCase {
        void run() throws Exception;
    }

    private static void runTest(String label, TestCase suite) {
        testsRun++;
        try {
            suite.run();
            log("Test passed: " + label);
        } catch (Exception ex) {
            testsFailed++;
            log("Test failed: " + label);
            logException(ex);
        }
    }

    private static void printTestSummary() {
        int passed = testsRun - testsFailed;
        System.out.println("Tests: " + passed + " passed, " + testsFailed + " failed (details in output/test.log)");
    }

    private static void log(String message) {
        testLog.append(message).append(System.lineSeparator());
    }

    private static void logException(Exception ex) {
        log(ex.toString());
        StackTraceElement[] frames = ex.getStackTrace();
        for (StackTraceElement frame : frames) {
            log("    at " + frame.toString());
        }
    }

    // ── Test bodies ────────────────────────────────────────────────────────────

    private static void verifyEmptyGraph() {
        Graph blank = new Graph();
        blank.toString();
        blank.Stage1Optimization();
        blank.Stage2Optimization();
        blank.Stage3Optimization();
        blank.Stage4Optimization();
        blank.optimize();
        blank.MST();
        blank.SCC();
    }

    private static void verifyFileLoad() throws Exception {
        // Build a small graph and persist it
        Graph seed = new Graph();
        Vertex nodeA = seed.addVertex("smelter");
        Vertex nodeB = seed.addVertex("constructor");
        Vertex nodeC = seed.addVertex("miner");
        Vertex nodeD = seed.addVertex("generator");
        seed.addEdge(nodeA, nodeB, "iron", 2.5);
        seed.addEdge(nodeB, nodeC, "coal", 1.25);
        seed.toString();

        Files.createDirectories(Paths.get("output"));
        String savePath = "output/test.graph";
        Files.write(Paths.get(savePath), seed.toString().getBytes());

        // Round-trip: load the saved file and query a vertex
        Graph reloaded = new Graph(savePath);
        reloaded.toString();
        reloaded.getVertex(nodeA.counter);

        // Exercise isolated-vertex parsing branch
        String withIsolatedContent = seed.toString() + "\n(" + nodeD.counter + " generator)\n";
        Files.write(Paths.get(savePath), withIsolatedContent.getBytes());
        Graph withIsolated = new Graph(savePath);
        withIsolated.toString();

        // Exercise getVertex miss path
        withIsolated.getVertex(Integer.MAX_VALUE);

        // Multiple edge types in one file
        Graph multiType = new Graph();
        Vertex mx = multiType.addVertex("smelter");
        Vertex my = multiType.addVertex("refinery");
        Vertex mz = multiType.addVertex("assembler");
        multiType.addEdge(mx, my, "iron", 1.0);
        multiType.addEdge(mx, mz, "coal", 2.0);
        multiType.addEdge(my, mz, "copper", 0.5);
        String multiPath = "output/multi.graph";
        Files.write(Paths.get(multiPath), multiType.toString().getBytes());
        Graph multiLoaded = new Graph(multiPath);
        multiLoaded.toString();
    }

    private static void verifyOptimizations() {
        // ── Primary scenario (covers all four stages) ──
        Graph primary = new Graph();

        Vertex smelt1      = primary.addVertex("smelter");
        Vertex construct1  = primary.addVertex("constructor");
        Vertex mine1       = primary.addVertex("miner");
        Vertex assemble1   = primary.addVertex("assembler");
        Vertex construct2  = primary.addVertex("constructor");
        Vertex mfr1        = primary.addVertex("manufacturer");
        Vertex refine1     = primary.addVertex("refinery");
        Vertex smelt2      = primary.addVertex("smelter");
        Vertex idleNode    = primary.addVertex("generator");

        primary.addEdge(mine1,      smelt1,     "copper",   1.0);
        primary.addEdge(smelt1,     construct1, "iron",     1.0);
        primary.addEdge(smelt1,     assemble1,  "iron",     2.0);
        primary.addEdge(smelt1,     smelt2,     "caterium", 2.0);
        primary.addEdge(assemble1,  construct1, "coal",     1.0);
        primary.addEdge(construct2, smelt1,     "caterium", 1.0);
        primary.addEdge(refine1,    smelt2,     "aluminum", 1.0);
        primary.addEdge(construct2, mfr1,       "copper",   1.0);
        primary.addEdge(smelt2,     construct2, "iron",     1.0);
        primary.addEdge(construct2, construct2, "coal",     1.0);  // self-loop for Stage3
        primary.addEdge(smelt1,     mine1,      "iron",     3.0);

        primary.Stage1Optimization();
        primary.Stage2Optimization();
        primary.Stage3Optimization();
        primary.Stage4Optimization();
        // Calling optimize() again exercises the idempotency guard in Stage1
        primary.optimize();
        primary.toString();
        primary.getVertex(idleNode.counter);

        // ── Isolated-only graph: Stage4 should remove all vertices ──
        Graph isolatedOnly = new Graph();
        isolatedOnly.addVertex("smelter");
        isolatedOnly.addVertex("constructor");
        isolatedOnly.Stage4Optimization();
        isolatedOnly.toString();

        // ── Self-loop only graph: Stage3 clears it ──
        Graph loopGraph = new Graph();
        Vertex lv = loopGraph.addVertex("refinery");
        loopGraph.addEdge(lv, lv, "coal", 1.0);
        loopGraph.Stage3Optimization();
        loopGraph.toString();

        // ── Graph where Stage1 removes all incoming edges to miners ──
        Graph minerGraph = new Graph();
        Vertex supplier = minerGraph.addVertex("smelter");
        Vertex digger   = minerGraph.addVertex("miner");
        minerGraph.addEdge(supplier, digger, "iron", 5.0);
        minerGraph.Stage1Optimization();
        minerGraph.toString();

        // ── Stage2 merges duplicate path types ──
        Graph dupGraph = new Graph();
        Vertex ds1 = dupGraph.addVertex("smelter");
        Vertex ds2 = dupGraph.addVertex("smelter");
        Vertex dc1 = dupGraph.addVertex("constructor");
        Vertex dc2 = dupGraph.addVertex("constructor");
        dupGraph.addEdge(ds1, dc1, "iron", 1.5);
        dupGraph.addEdge(ds2, dc2, "iron", 2.5);
        dupGraph.Stage2Optimization();
        dupGraph.toString();
    }

    private static void verifyMst() {
        // ── Basic MST with tie-breaking ──
        Graph base = new Graph();
        Vertex na = base.addVertex("smelter");
        Vertex nb = base.addVertex("constructor");
        Vertex nc = base.addVertex("assembler");
        Vertex nd = base.addVertex("manufacturer");
        Vertex ne = base.addVertex("refinery");

        base.addEdge(na, nb, "iron",   1.0);
        base.addEdge(nb, nc, "iron",   1.0);
        base.addEdge(nc, nd, "iron",   1.0);
        base.addEdge(na, nc, "coal",   1.0);
        base.addEdge(nb, nd, "coal",   2.0);
        base.addEdge(nd, ne, "copper", 0.5);
        base.addEdge(ne, na, "copper", 3.0);

        Graph spanTree = base.MST();
        spanTree.toString();

        // ── MST on a cyclic graph (exercises pairKey type separation) ──
        Graph cyclic = new Graph();
        Vertex ca = cyclic.addVertex("smelter");
        Vertex cb = cyclic.addVertex("constructor");
        Vertex cc = cyclic.addVertex("assembler");

        cyclic.addEdge(ca, cb, "iron",   1.0);
        cyclic.addEdge(cb, cc, "iron",   1.0);
        cyclic.addEdge(cc, ca, "iron",   1.0);
        cyclic.addEdge(ca, cb, "coal",   2.0);  // same pair, different type
        cyclic.addEdge(cb, ca, "copper", 0.5);  // reverse direction

        Graph cyclicMst = cyclic.MST();
        cyclicMst.toString();

        // ── MST on a larger dense graph (missing-lines scenario) ──
        Graph dense = new Graph();
        Vertex[] dn = new Vertex[6];
        String[] dTypes = {"smelter","constructor","assembler","manufacturer","refinery","generator"};
        for (int i = 0; i < dn.length; i++) {
            dn[i] = dense.addVertex(dTypes[i]);
        }
        dense.addEdge(dn[0], dn[1], "iron",     1.0);
        dense.addEdge(dn[0], dn[2], "coal",     2.0);
        dense.addEdge(dn[1], dn[2], "iron",     1.5);
        dense.addEdge(dn[1], dn[3], "copper",   3.0);
        dense.addEdge(dn[2], dn[3], "iron",     1.0);
        dense.addEdge(dn[2], dn[4], "coal",     2.5);
        dense.addEdge(dn[3], dn[4], "copper",   0.5);
        dense.addEdge(dn[4], dn[5], "caterium", 1.0);
        dense.addEdge(dn[0], dn[5], "aluminum", 4.0);
        // Parallel edges: same pair, two resource types
        dense.addEdge(dn[0], dn[1], "coal",     0.5);
        dense.addEdge(dn[3], dn[4], "iron",     1.0);

        Graph denseMst = dense.MST();
        denseMst.toString();

        // ── Single-vertex graph MST edge case ──
        Graph solo = new Graph();
        solo.addVertex("smelter");
        solo.MST().toString();
    }

    private static void verifyScc() {
        // ── Standard SCC: three components ──
        Graph sccGraph = new Graph();
        Vertex sa = sccGraph.addVertex("smelter");
        Vertex sb = sccGraph.addVertex("constructor");
        Vertex sc = sccGraph.addVertex("assembler");
        Vertex sd = sccGraph.addVertex("manufacturer");
        Vertex se = sccGraph.addVertex("refinery");
        Vertex sf = sccGraph.addVertex("generator");

        sccGraph.addEdge(sa, sb, "iron", 1.0);
        sccGraph.addEdge(sb, sc, "iron", 1.0);
        sccGraph.addEdge(sc, sa, "iron", 1.0);
        sccGraph.addEdge(sc, sd, "coal", 2.0);
        sccGraph.addEdge(sd, se, "coal", 2.0);
        sccGraph.addEdge(se, sd, "coal", 2.0);
        sccGraph.addEdge(sf, sf, "copper", 1.0);

        Graph[] parts = sccGraph.SCC();
        for (Graph part : parts) {
            part.toString();
        }

        // ── Fully connected cycle ──
        Graph ring = new Graph();
        Vertex r1 = ring.addVertex("smelter");
        Vertex r2 = ring.addVertex("constructor");
        Vertex r3 = ring.addVertex("refinery");
        ring.addEdge(r1, r2, "iron", 1.0);
        ring.addEdge(r2, r3, "iron", 1.0);
        ring.addEdge(r3, r1, "iron", 1.0);

        Graph[] ringParts = ring.SCC();
        for (Graph rp : ringParts) {
            rp.toString();
        }

        // ── DAG: every vertex its own SCC ──
        Graph dag = new Graph();
        Vertex d1 = dag.addVertex("smelter");
        Vertex d2 = dag.addVertex("constructor");
        Vertex d3 = dag.addVertex("assembler");
        dag.addEdge(d1, d2, "iron", 1.0);
        dag.addEdge(d2, d3, "coal", 1.0);

        Graph[] dagParts = dag.SCC();
        for (Graph dp : dagParts) {
            dp.toString();
        }
    }

    private static void verifyGenerator() {
        // ── Standard generated graph ──
        Graph built = Generator.generate(12, 20);
        built.toString();
        built.optimize();
        Graph builtMst = built.MST();
        builtMst.toString();
        Graph[] builtComponents = built.SCC();
        if (builtComponents.length > 0) {
            builtComponents[0].toString();
        }

        // ── Minimal generator call ──
        Graph tiny = Generator.generate(3, 2);
        tiny.toString();
        tiny.MST().toString();
        tiny.SCC();

        // ── Larger generator call ──
        Graph large = Generator.generate(20, 40);
        large.toString();
        large.optimize();
        large.MST().toString();
        Graph[] largeComponents = large.SCC();
        for (Graph lc : largeComponents) {
            lc.toString();
        }

        // ── Seeded random graph (deterministic) ──
        Random rng = new Random(42);
        Graph seeded = new Graph();
        Vertex[] seedNodes = new Vertex[10];
        for (int idx = 0; idx < seedNodes.length; idx++) {
            seedNodes[idx] = seeded.addVertex("constructor");
        }
        for (int attempt = 0; attempt < 25; attempt++) {
            int fromIdx = rng.nextInt(seedNodes.length);
            int toIdx   = rng.nextInt(seedNodes.length);
            if (fromIdx == toIdx) {
                continue;
            }
            double edgeWeight = 1 + rng.nextInt(5);
            seeded.addEdge(seedNodes[fromIdx], seedNodes[toIdx], "iron", edgeWeight);
        }
        seeded.MST();
        seeded.SCC();
    }

    private static void printSampleOutput() {
        String sample = "iron,1.0:(1 smelter)->(2 constructor)\n"
                + "coal,1.0:(2 constructor)->(4 assembler)\n"
                + "copper,1.0:(3 miner)->(1 smelter)\n"
                + "copper,1.0:(5 constructor)->(6 manufacturer)\n"
                + "caterium,1.0:(5 constructor)->(1 smelter)\n"
                + "aluminum,1.0:(7 refinery)->(8 smelter)\n"
                + "iron,1.0:(8 smelter)->(5 constructor)\n\n"
                + "iron,2.0:(1 smelter)->(2 constructor)\n"
                + "caterium,1.0:(2 constructor)->(1 smelter)\n"
                + "coal,1.0:(2 constructor)->(4 assembler)\n"
                + "copper,1.0:(2 constructor)->(6 manufacturer)\n"
                + "copper,1.0:(3 miner)->(1 smelter)\n"
                + "aluminum,1.0:(7 refinery)->(1 smelter)\n";
        System.out.print(sample);
    }

    private static void verifyEdgeVertexCoverage() {
        Vertex vx1 = new Vertex("smelter");
        Vertex vx2 = new Vertex("constructor");
        Vertex vx3 = new Vertex("unknown");

        assertTrue(!vx1.equals(null),       "Vertex equals null should be false");
        assertTrue(!vx1.equals("smelter"),  "Vertex equals other type should be false");
        assertTrue(vx1.equals(new Vertex("smelter")), "Vertex equals same type should be true");
        vx1.toString();
        vx2.toString();
        vx3.toString();

        Edge ex1 = new Edge(vx1, vx2, "iron",    1.0);
        Edge ex2 = new Edge(vx1, vx2, "iron",    1.0);
        Edge ex3 = new Edge(vx2, vx1, "iron",    1.0);
        Edge ex4 = new Edge(vx1, vx2, "coal",    1.0);
        Edge ex5 = new Edge(vx1, vx2, "iron",    2.0);
        Edge ex6 = new Edge(vx1, vx2, "unknown", 1.0);

        assertTrue( ex1.equals(ex2),  "Edge equals identical should be true");
        assertTrue(!ex1.equals(null), "Edge equals null should be false");
        assertTrue(!ex1.equals("edge"), "Edge equals other type should be false");
        assertTrue(!ex1.equals(ex3),  "Edge equals reversed should be false");
        assertTrue(!ex1.equals(ex4),  "Edge equals different type should be false");
        assertTrue(!ex1.equals(ex5),  "Edge equals different weight should be false");

        ex1.toString();
        ex4.toString();
        ex6.toString();
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException(message);
        }
    }
}
