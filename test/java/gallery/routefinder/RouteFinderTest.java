package java.gallery.routefinder;

import gallery.routefinder.algorithm.RouteFinder;
import gallery.routefinder.graph.Graph;
import gallery.routefinder.graph.GraphNode;
import gallery.routefinder.model.Room;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
class RouteFinderTest {
    private Graph graph = new Graph();
    private GraphNode startNode;
    private GraphNode endNode;
    private GraphNode firstWaypoint;
    private GraphNode secondWaypoint;
    private GraphNode spareNode;
    private GraphNode testNode;
    private RouteFinder routeFinder;
    private List<List<GraphNode>> foundRoutes = new ArrayList<>();
    private List<GraphNode> waypoints = new ArrayList<>();
    private Set<String> avoid = new HashSet<>();
    private int max;

    RouteFinderTest(GraphNode startNode, GraphNode endNode, int max, GraphNode firstWaypoint, GraphNode secondWaypoint, GraphNode testNode, RouteFinder routeFinder) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.firstWaypoint = firstWaypoint;
        this.secondWaypoint = secondWaypoint;
        this.testNode = testNode;
        this.max = max;
        this.routeFinder = routeFinder;
    }

    @BeforeEach
    void setUp() {
        startNode = new GraphNode(new Room("start", "start", 10, 5));
        spareNode = new GraphNode(new Room("spare", "spare", 11, 6));
        firstWaypoint = new GraphNode(new Room("firstWaypoint", "firstWaypoint", 12, 7));
        secondWaypoint = new GraphNode(new Room("secondWaypoint", "secondWaypoint", 13, 9));
        testNode = new GraphNode(new Room("avoidOne", "avoidOne", 16, 3));
        endNode = new GraphNode(new Room("end", "end", 15, 12));

        startNode.addEdge(firstWaypoint,5);
        firstWaypoint.addEdge(startNode,5);
        startNode.addEdge(secondWaypoint,7);
        secondWaypoint.addEdge(startNode,7);
        startNode.addEdge(testNode,2);
        testNode.addEdge(startNode,2);

        spareNode.addEdge(startNode,6);
        startNode.addEdge(startNode,6);
        spareNode.addEdge(firstWaypoint,6);
        firstWaypoint.addEdge(spareNode,6);

        firstWaypoint.addEdge(secondWaypoint,2);
        secondWaypoint.addEdge(firstWaypoint,2);

        endNode.addEdge(firstWaypoint,3);
        firstWaypoint.addEdge(endNode,3);
        endNode.addEdge(secondWaypoint,8);
        secondWaypoint.addEdge(endNode,8);
        endNode.addEdge(testNode,4);
        testNode.addEdge(endNode,4);

        graph.addNode(startNode);
        graph.addNode(endNode);
        graph.addNode(testNode);
        graph.addNode(firstWaypoint);
        graph.addNode(secondWaypoint);

        waypoints.add(firstWaypoint);
        waypoints.add(secondWaypoint);

        avoid.add("avoidOne");
        max=2;
    }

    @AfterEach
    void tearDown() {

    }

    @Test
    void findSingleRoute() {
        assertTrue(foundRoutes.isEmpty());
        RouteFinder.findSingleRoute(startNode,endNode,avoid,waypoints);
        assertEquals(1, foundRoutes.size());
        assertTrue(foundRoutes.contains(waypoints));
        assertTrue(foundRoutes.contains(endNode));
    }

    @Test
    void findMultipleRoutes() {
    }

    @Test
    void bfsShortestPath() {
    }

    @Test
    void dijkstraShortestPath() {
    }

    @Test
    void mostInterestingRoute() {
    }
}