
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
        try {
            this.type = EdgeType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.type = EdgeType.UNDEFINED;
        }

    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Edge)) return false;
        Edge e = (Edge) obj;
        return this.v1.equals(e.v1) &&
               this.v2.equals(e.v2) &&
               this.type == e.type &&
               Double.compare(this.weight, e.weight) == 0;
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