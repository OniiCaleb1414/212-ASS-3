
enum EdgeType {
    IRON, COAL, COPPER, CATERIUM, ALUMINUM, UNDEFINED
}

public class Edge {
    Vertex v1;
    Vertex v2;
    double weight;
    EdgeType type;

    public Edge(Vertex v1, Vertex v2, String type, double weight) {
        
    }

    @Override
    public boolean equals(Object obj) {

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