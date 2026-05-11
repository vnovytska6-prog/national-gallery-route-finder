package gallery.routefinder;

import gallery.routefinder.algorithm.RouteFinder;
import gallery.routefinder.graph.Graph;
import gallery.routefinder.graph.GraphNode;
import gallery.routefinder.model.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
class RouteFinderTest {
    private Graph graph = new Graph();
    private GraphNode start;
    private GraphNode end;
    private GraphNode waypoint;
    private GraphNode avoidNode;
    private Set<String> avoidRooms;
    private List<GraphNode> waypoints;
    private Set<String> favoriteArtists;


    @BeforeEach
    void setUp() {
        // test graph
        graph = new Graph();
        // nodes
        start= new GraphNode(new Room("start", "start", 10, 5));
        waypoint = new GraphNode(new Room("wp1", "Waypoint 1", 12, 7));
        avoidNode = new GraphNode(new Room("avoid", "Avoid Room", 16, 3));
        end = new GraphNode(new Room("end", "End Room", 15, 12));
        GraphNode otherPath = new GraphNode(new Room("other", "Other Path", 20, 20));

        // add nodes to graph
        graph.addNode(start);
        graph.addNode(waypoint);
        graph.addNode(avoidNode);
        graph.addNode(end);
        graph.addNode(otherPath);

        // add edges
        // start -> waypoint -> end
        start.addEdge(waypoint, 5);
        waypoint.addEdge(start, 5);
        waypoint.addEdge(end, 3);
        end.addEdge(waypoint, 3);

        // start -> otherPath -> end (longer)
        start.addEdge(otherPath, 7);
        otherPath.addEdge(start, 7);
        otherPath.addEdge(end, 8);
        end.addEdge(otherPath, 8);

        // path through avoid node (cheapest but should be avoided)
        start.addEdge(avoidNode, 2);
        avoidNode.addEdge(start, 2);
        avoidNode.addEdge(end, 4);
        end.addEdge(avoidNode, 4);

        // test data setup
        avoidRooms = new HashSet<>();
        avoidRooms.add("avoid");

        waypoints = new ArrayList<>();
        waypoints.add(waypoint);

        favoriteArtists = new HashSet<>();
        favoriteArtists.add("rembrandt");
        favoriteArtists.add("van gogh");
    }

    // T1 ANY SINGLE ROUTE
    @Test
    void testFindSingleRoute_ShouldFindValidRoute() {
        List<GraphNode> route = RouteFinder.findSingleRoute(start, end, null, null);

        assertNotNull(route);
        assertEquals(start, route.get(0));
        assertEquals(end, route.get(route.size() - 1));
    }

    @Test
    void testFindSingleRoute_WithWaypoint_ShouldIncludeWaypoint() {
        List<GraphNode> route = RouteFinder.findSingleRoute(start, end, null, waypoints);

        assertNotNull(route);
        assertTrue(route.contains(waypoint), "Route must contain the waypoint");
    }

    @Test
    void testFindSingleRoute_WithAvoid_ShouldNotContainAvoidedRoom() {
        List<GraphNode> route = RouteFinder.findSingleRoute(start, end, avoidRooms, null);

        assertNotNull(route);
        assertFalse(route.contains(avoidNode), "Route must not contain avoided room");
    }

    // T2 MULTIPLE ROUTES (DFS)
    @Test
    void testFindMultipleRoutes_ShouldReturnMultiplePaths() {
        List<List<GraphNode>> routes = RouteFinder.findMultipleRoutes(start, end, 3, null, null, null, 0, 10);

        assertNotNull(routes);
        assertTrue(routes.size() >= 2, "Should find at least 2 different paths");

        // Verify each route starts at start and ends at end
        for (List<GraphNode> route : routes) {
            assertEquals(start, route.get(0));
            assertEquals(end, route.get(route.size() - 1));
        }
    }

    @Test
    void testFindMultipleRoutes_RespectsMaxLimit() {
        int maxRoutes = 2;
        List<List<GraphNode>> routes = RouteFinder.findMultipleRoutes(start, end, maxRoutes, null, null, null, 0, 10);

        assertNotNull(routes);
        assertTrue(routes.size() <= maxRoutes);
    }

    // T3 BFS SHORTEST PATH
    @Test
    void testBfsShortestPath_ShouldReturnShortestRoute() {
        List<GraphNode> route = RouteFinder.bfsShortestPath(start, end, null, null);

        assertNotNull(route);
        assertEquals(start, route.get(0));
        assertEquals(end, route.get(route.size() - 1));
    }

    @Test
    void testBfsShortestPath_NoPath_ReturnsNull() {
        GraphNode isolated = new GraphNode(new Room("iso", "Isolated", 99, 99));
        graph.addNode(isolated);

        List<GraphNode> route = RouteFinder.bfsShortestPath(start, isolated, null, null);

        assertNull(route);
    }

    // T4 DIJKSTRA SHORTEST PATH
    @Test
    void testDijkstraShortestPath_ShouldFindPath() {
        List<GraphNode> route = RouteFinder.dijkstraShortestPath(start, end, null, null);

        assertNotNull(route);
        assertEquals(start, route.get(0));
        assertEquals(end, route.get(route.size() - 1));
    }

    @Test
    void testDijkstraShortestPath_WithAvoid_ShouldSkipAvoidedRooms() {
        List<GraphNode> route = RouteFinder.dijkstraShortestPath(start, end, avoidRooms, null);

        assertNotNull(route);
        assertFalse(route.contains(avoidNode), "Route must skip avoided rooms");
    }

    // T5 MOST INTERESTING ROUTE
    @Test
    void testMostInterestingRoute_ShouldWorkWithArtists() {
        List<GraphNode> route = RouteFinder.mostInterestingRoute(graph, start, end, favoriteArtists, null, null);

        assertNotNull(route);
        assertEquals(start, route.get(0));
        assertEquals(end, route.get(route.size() - 1));
    }

    // T6: ROUTE DISTANCE
    @Test
    void testCalculateRouteDistance_ShouldReturnCorrectSum() {
        List<GraphNode> route = Arrays.asList(start, waypoint, end);
        double distance = RouteFinder.calculateRouteDistance(route);

        assertEquals(8.0, distance, 0.001); // 8
    }

    @Test
    void testCalculateRouteDistance_EmptyRoute_ReturnsZero() {
        double distance = RouteFinder.calculateRouteDistance(new ArrayList<>());
        assertEquals(0.0, distance);
    }
}
