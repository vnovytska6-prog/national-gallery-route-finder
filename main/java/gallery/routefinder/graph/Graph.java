package gallery.routefinder.graph;

import gallery.routefinder.model.Room;
import java.util.*;

public class Graph {
    private Map<String, GraphNode> nodeMap;
    private List<GraphNode> allNodes;

    public Graph() {
        this.nodeMap = new HashMap<>();
        this.allNodes = new ArrayList<>();
    }

    public void addNode(GraphNode node) {
        nodeMap.put(node.getRoom().getId(), node);
        allNodes.add(node);
    }

    public GraphNode getNodeById(String id) {
        return nodeMap.get(id);
    }

    public List<GraphNode> getAllNodes() {
        return allNodes;
    }

    public void addUndirectedEdge(String fromId, String toId, double distance) {
        GraphNode from = nodeMap.get(fromId);
        GraphNode to = nodeMap.get(toId);
        if (from != null && to != null) {
            from.addEdge(to, distance);
            to.addEdge(from, distance);
        }
    }

    public void resetForSearch() {
        for (GraphNode node : allNodes) {
            node.setDistanceFromStart(Double.POSITIVE_INFINITY);
            node.setVisited(false);
            node.setPrevious(null);
        }
    }

    public GraphNode getNodeByRoomName(String name) {
        for (GraphNode node : allNodes) {
            if (node.getRoom().getName().equalsIgnoreCase(name)) {
                return node;
            }
            if (node.getRoom().getId().equalsIgnoreCase(name)) {
                return node;
            }
        }
        return null;
    }

    public GraphNode getNodeByArtworkTitle(String title) {
        for (GraphNode node : allNodes) {
            for (var artwork : node.getRoom().getArtworks()) {
                if (artwork.getTitle().equalsIgnoreCase(title)) {
                    return node;
                }
            }
        }
        return null;
    }

    public List<GraphNode> getNodesByArtist(String artist) {
        List<GraphNode> result = new ArrayList<>();
        for (GraphNode node : allNodes) {
            for (var artwork : node.getRoom().getArtworks()) {
                if (artwork.getArtist().equalsIgnoreCase(artist)) {
                    result.add(node);
                    break;
                }
            }
        }
        return result;
    }
}