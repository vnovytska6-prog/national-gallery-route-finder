package gallery.routefinder.graph;

import gallery.routefinder.model.Room;
import java.util.ArrayList;
import java.util.List;

public class GraphNode {
    private Room room;
    private List<GraphEdge> edges;
    private double distanceFromStart;
    private boolean visited;
    private GraphNode previous;

    public GraphNode(Room room) {
        this.room = room;
        this.edges = new ArrayList<>();
        this.distanceFromStart = Double.POSITIVE_INFINITY;
        this.visited = false;
        this.previous = null;
    }

    public void addEdge(GraphNode destination, double distance) {
        edges.add(new GraphEdge(destination, distance));
    }

    // Getters and setters
    public Room getRoom() { return room; }
    public List<GraphEdge> getEdges() { return edges; }
    public double getDistanceFromStart() { return distanceFromStart; }
    public void setDistanceFromStart(double distance) { this.distanceFromStart = distance; }
    public boolean isVisited() { return visited; }
    public void setVisited(boolean visited) { this.visited = visited; }
    public GraphNode getPrevious() { return previous; }
    public void setPrevious(GraphNode previous) { this.previous = previous; }

    @Override
    public String toString() {
        return room.getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GraphNode other = (GraphNode) obj;
        return room.getId().equals(other.room.getId());
    }

    @Override
    public int hashCode() { return room.getId().hashCode(); }
}