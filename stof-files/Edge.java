
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

        String value = type == null ? "" : type.trim().toLowerCase();
        if (value.endsWith(".png")) {
            value = value.substring(0, value.length() - 4);
        }
        switch (value) {
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
                break;
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
        return this.v1.equals(other.v1)
                && this.v2.equals(other.v2)
                && this.type == other.type
                && Double.compare(this.weight, other.weight) == 0;
    }

    @Override
    public String toString() {
        String image = "";
        switch (type) {
            case IRON:
                image = "iron.png";
                break;
            case COAL:
                image = "coal.png";
                break;
            case COPPER:
                image = "copper.png";
                break;
            case CATERIUM:
                image = "caterium.png";
                break;
            case ALUMINUM:
                image = "aluminum.png";
                break;
            default:
                image = "undefined.png";
                break;
        }
        return image + "," + weight;
    }
}
