package gallery.routefinder.graph;

import gallery.routefinder.model.Room;
import gallery.routefinder.model.Artwork;
import java.util.*;

public class Graph {
    private Map<String, GraphNode> nodeMap;     // fast lookup by id
    private List<GraphNode> allNodes;            // all nodes in the graph

    public Graph() {
        this.nodeMap = new HashMap<>();
        this.allNodes = new ArrayList<>();
    }

    // adds a node to the graph
    public void addNode(GraphNode node) {
        nodeMap.put(node.getRoom().getId(), node);
        allNodes.add(node);
    }

    // returns node by its id, or null if not found
    public GraphNode getNodeById(String id) {
        return nodeMap.get(id);
    }
    // returns list of all nodes
    public List<GraphNode> getAllNodes() {
        return allNodes;
    }
    // adds a two-way connection between rooms
    public void addUndirectedEdge(String fromId, String toId, double distance) {
        GraphNode from = nodeMap.get(fromId);
        GraphNode to = nodeMap.get(toId);
        if (from != null && to != null) {
            from.addEdge(to, distance);
            to.addEdge(from, distance);
        }
    }

    // resets node values before running a new search
    public void resetForSearch() {
        for (GraphNode node : allNodes) {
            node.setDistanceFromStart(Double.POSITIVE_INFINITY);
            node.setVisited(false);
            node.setPrevious(null);
        }
    }

    // finds node by room name or id
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

    // finds node that contains artwork with given title
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

    // returns all rooms that contain artwork by given artist
    public List<GraphNode> getNodesByArtist(String artist) {
        List<GraphNode> result = new ArrayList<>();
        String artistLower = artist.toLowerCase();
        for (GraphNode node : allNodes) {
            for (Artwork artwork : node.getRoom().getArtworks()) {
                if (artwork.getArtist().toLowerCase().contains(artistLower) ||
                        artistLower.contains(artwork.getArtist().toLowerCase())) {
                    result.add(node);
                    break;
                }
            }
        }
        return result;}
}