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

        String typeLowercase = type.toLowerCase();
        switch (typeLowercase) {
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
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Edge anotherEdge = (Edge) obj;

        if (Double.compare(anotherEdge.weight, weight) != 0) return false;
        if (type != anotherEdge.type) return false;
        if (!v1.equals(anotherEdge.v1)) return false;
        if (!v2.equals(anotherEdge.v2)) return false;

        return true;
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