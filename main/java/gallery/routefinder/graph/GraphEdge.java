package gallery.routefinder.graph;

public class GraphEdge {
    private GraphNode destination;
    private double distance;

    public GraphEdge(GraphNode destination, double distance) {
        this.destination = destination;
        this.distance = distance;
    }

    public GraphNode getDestination() { return destination; }
    public double getDistance() { return distance; }
}

