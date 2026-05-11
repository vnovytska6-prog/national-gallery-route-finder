package gallery.routefinder;

import gallery.routefinder.db.DataLoader;
import gallery.routefinder.graph.Graph;
import gallery.routefinder.graph.GraphNode;
import gallery.routefinder.algorithm.RouteFinder;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
public class BenchmarkRunner {

    private Graph graph;
    private GraphNode startNode;
    private GraphNode endNode;
    private Set<String> avoidRooms;
    private List<GraphNode> waypoints;
    private Set<String> favoriteArtists;

    // loads data once before all benchmarks
    @Setup(Level.Trial)
    public void setup() throws Exception {
        graph = DataLoader.loadFromCSV(
                "rooms.csv",
                "connections.csv",
                "artworks.csv"
        );

        // test with two distant rooms
        startNode = graph.getNodeById("1");
        endNode = graph.getNodeById("66");

        avoidRooms = new HashSet<>();
        avoidRooms.add("15");
        avoidRooms.add("20");

        waypoints = Arrays.asList(
                graph.getNodeById("30"),
                graph.getNodeById("45")
        );

        favoriteArtists = new HashSet<>(Arrays.asList("rembrandt", "van gogh", "titian"));
    }

    @Benchmark
    public void benchmarkAnyRoute() {
        RouteFinder.findSingleRoute(startNode, endNode, avoidRooms, waypoints);
    }

    @Benchmark
    public void benchmarkMultipleRoutes() {
        RouteFinder.findMultipleRoutes(startNode, endNode, 5, avoidRooms, waypoints, null, 0, 15);
    }

    @Benchmark
    public void benchmarkDijkstra() {
        RouteFinder.dijkstraShortestPath(startNode, endNode, avoidRooms, waypoints);
    }

    @Benchmark
    public void benchmarkBFSGraph() {
        RouteFinder.bfsShortestPath(startNode, endNode, avoidRooms, waypoints);
    }

    @Benchmark
    public void benchmarkInterestingRoute() {
        RouteFinder.mostInterestingRoute(graph, startNode, endNode, favoriteArtists, avoidRooms, waypoints);
    }

    @Benchmark
    public void benchmarkCalculateRouteDistance() {
        // Создаем простой маршрут для теста
        List<GraphNode> route = Arrays.asList(startNode, endNode);
        RouteFinder.calculateRouteDistance(route);
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(BenchmarkRunner.class.getSimpleName())
                .build();

        new Runner(options).run();
    }
}