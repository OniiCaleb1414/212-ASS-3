
enum EdgeType {
    IRON, COAL, COPPER, CATERIUM, ALUMINUM, UNDEFINED
}

public class Edge {
    Vertex v1;
    Vertex v2;
    double weight;
    EdgeType type;

    public Edge(Vertex v1, Vertex v2, String type, double weight) {
        this.v1 = v1;
        this.v2 = v2;
        this.weight = weight;
        if (type == null) {
            this.type = EdgeType.UNDEFINED;
            return;
        }
        switch (type.toLowerCase()) {
            case "iron":
                this.type = EdgeType.IRON;
                break;
            case "coal":
                this.type = EdgeType.COAL;
                break;
            case "copper":
                this.type = EdgeType.COPPER;
                break;
            case "caterium":
                this.type = EdgeType.CATERIUM;
                break;
            case "aluminum":
                this.type = EdgeType.ALUMINUM;
                break;
            default:
                this.type = EdgeType.UNDEFINED;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Edge)) {
            return false;
        }
        Edge other = (Edge) obj;
        if (v1 == null || v2 == null || other.v1 == null || other.v2 == null) {
            return false;
        }
        return v1.counter == other.v1.counter
                && v2.counter == other.v2.counter
                && type == other.type
                && Double.compare(weight, other.weight) == 0;
    }

    @Override
    public String toString() {
        String image = "";
        switch (type) {
            case IRON:
                image = "iron";
                break;
            case COAL:
                image = "coal";
                break;
            case COPPER:
                image = "copper";
                break;
            case CATERIUM:
                image = "caterium";
                break;
            case ALUMINUM:
                image = "aluminum";
                break;
            default:
                image = "undefined";
                break;
        }
        return image + "," + weight;
    }
}