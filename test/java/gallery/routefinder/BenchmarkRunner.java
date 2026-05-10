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

//JMH Benchmark for RouteFinder algorith performance of different pathfinding algorithms
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
public class BenchmarkRunner {

    private Graph graph;
    private RouteFinder routeFinder;
    private GraphNode startNode;
    private GraphNode endNode;
    private Set<String> avoidRooms;
    private List<GraphNode> waypoints;
    private Set<String> favoriteArtists;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        graph = DataLoader.loadFromCSV(
                "rooms.csv",
                "connections.csv",
                "artworks.csv"
        );
        routeFinder = new RouteFinder();

        // Pick two far apart rooms for realistic testing
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
        routeFinder.finalize(graph, startNode, endNode, avoidRooms, waypoints);
    }

    @Benchmark
    public void benchmarkMultipleRoutes() {
        routeFinder.findMultipleRoutes(graph, startNode, endNode, 10, avoidRooms, waypoints);
    }

    @Benchmark
    public void benchmarkDijkstra() {
        routeFinder.dijkstraShortestPath(graph, startNode, endNode, avoidRooms, waypoints);
    }

    @Benchmark
    public void benchmarkBFSGraph() {
        routeFinder.bfsShortestPath(graph, startNode, endNode, avoidRooms, waypoints);
    }

    @Benchmark
    public void benchmarkInterestingRoute() {
        routeFinder.mostInterestingRoute(graph, startNode, endNode, favoriteArtists, avoidRooms, waypoints);
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(BenchmarkRunner.class.getSimpleName())
                .output("benchmark_results.txt")
                .build();

        new Runner(options).run();
    }
}
