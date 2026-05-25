import java.util.ArrayList;

enum VertexType {
    MINER, SMELTER, CONSTRUCTOR, ASSEMBLER, MANUFACTURER, REFINERY, GENERATOR, UNDEFINED
}

public class Vertex {
    ArrayList<Edge> edges = new ArrayList<>();
    static int globalCounter = 1;
    int counter = globalCounter++;
    VertexType type;

    public Vertex(String type) {
        
    }

    @Override
    public boolean equals(Object obj) {

    }

    @Override
    public String toString() {
        String image = "";
        switch (type) {
            case MINER:
                image = "miner";
                break;
            case SMELTER:
                image = "smelter";
                break;
            case CONSTRUCTOR:
                image = "constructor";
                break;
            case ASSEMBLER:
                image = "assembler";
                break;
            case MANUFACTURER:
                image = "manufacturer";
                break;
            case REFINERY:
                image = "refinery";
                break;
            case GENERATOR:
                image = "generator";
                break;
            default:
                image = "undefined";
        }

        return "(" + counter + " " + image + ")";
    }

}