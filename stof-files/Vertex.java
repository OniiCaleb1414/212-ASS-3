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
        String value = type == null ? "" : type.trim().toLowerCase();
        switch (value) {
            case "miner":
                this.type = VertexType.MINER;
                break;
            case "smelter":
                this.type = VertexType.SMELTER;
                break;
            case "constructor":
                this.type = VertexType.CONSTRUCTOR;
                break;
            case "assembler":
                this.type = VertexType.ASSEMBLER;
                break;
            case "manufacturer":
                this.type = VertexType.MANUFACTURER;
                break;
            case "refinery":
                this.type = VertexType.REFINERY;
                break;
            case "generator":
                this.type = VertexType.GENERATOR;
                break;
            default:
                this.type = VertexType.UNDEFINED;
                break;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Vertex)) {
            return false;
        }
        Vertex other = (Vertex) obj;
        return this.type == other.type;
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