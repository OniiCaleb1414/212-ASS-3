
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
        boolean v1Equal = v1 == null ? other.v1 == null : v1.equals(other.v1);
        boolean v2Equal = v2 == null ? other.v2 == null : v2.equals(other.v2);
        return v1Equal
                && v2Equal
                && type == other.type
                && Double.compare(weight, other.weight) == 0;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (type == null ? 0 : type.hashCode());
        result = 31 * result + (v1 == null ? 0 : v1.hashCode());
        result = 31 * result + (v2 == null ? 0 : v2.hashCode());
        long bits = Double.doubleToLongBits(weight);
        result = 31 * result + (int) (bits ^ (bits >>> 32));
        return result;
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