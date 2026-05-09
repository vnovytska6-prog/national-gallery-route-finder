package gallery.routefinder;

import gallery.routefinder.db.DataLoader;
import gallery.routefinder.graph.Graph;
import gallery.routefinder.graph.GraphNode;
import gallery.routefinder.algorithm.RouteFinder;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.*;
        import java.io.InputStream;

public class Main extends Application {

    private Graph graph;
    private RouteFinder routeFinder;
    private MapCanvas mapCanvas;

    // UI Components
    private ComboBox<String> startCombo;
    private ComboBox<String> endCombo;
    private ListView<String> waypointsList;
    private ListView<String> avoidList;
    private TextField artistField;
    private TextArea routeInfoArea;
    private ListView<String> routesListView;
    private Label statusLabel;

    // Data
    private Map<String, GraphNode> displayToNode = new HashMap<>();
    private Set<String> avoidRooms = new HashSet<>();
    private Set<String> favoriteArtists = new HashSet<>();
    private List<GraphNode> waypoints = new ArrayList<>();
    private List<List<GraphNode>> foundRoutes = new ArrayList<>();
    private List<String> roomDisplayNames = new ArrayList<>();

    public static void main(String[] args) {
        // Выбор режима: если аргумент "console" - консоль, иначе - GUI
        if (args.length > 0 && args[0].equals("console")) {
            runConsoleMode();
        } else {
            launch(args);
        }
    }

    // Консольный режим (твой старый код)
    private static void runConsoleMode() {
        System.out.println("Gallery Route Finder\n");
        try {
            Graph graph = DataLoader.loadFromCSV(
                    "rooms.csv",
                    "connections.csv",
                    "artworks.csv"
            );
            System.out.println("Loaded rooms: " + graph.getAllNodes().size());
            for (GraphNode node : graph.getAllNodes()) {
                System.out.println("  📍 " + node.getRoom().getName());
                for (var artwork : node.getRoom().getArtworks()) {
                    System.out.println("     🖼️ " + artwork);
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // GUI режим
    @Override
    public void start(Stage primaryStage) throws Exception {
        showLoadingScreen(primaryStage);

        new Thread(() -> {
            try {
                loadData();
                Platform.runLater(() -> {
                    showMainWindow(primaryStage);
                    statusLabel.setText("Ready | " + graph.getAllNodes().size() + " rooms loaded");
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError(primaryStage, e.getMessage()));
            }
        }).start();
    }

    private void loadData() throws Exception {
        String basePath = System.getProperty("user.dir") + "/src/main/resources/";
        graph = DataLoader.loadFromCSV(
                "rooms.csv",
                "connections.csv",
                "artworks.csv"
        );
        routeFinder = new RouteFinder();

        for (GraphNode node : graph.getAllNodes()) {
            String display = node.getRoom().getName() + " [" + node.getRoom().getId() + "]";
            roomDisplayNames.add(display);
            displayToNode.put(display, node);
        }
        Collections.sort(roomDisplayNames);
    }

    private void showLoadingScreen(Stage stage) {
        VBox loadingBox = new VBox(20);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setStyle("-fx-background-color: #1a1a2e;");

        Label title = new Label("🏛️ NATIONAL GALLERY");
        title.setFont(Font.font("Arial", 36));
        title.setTextFill(Color.WHITE);

        ProgressIndicator progress = new ProgressIndicator();
        loadingBox.getChildren().addAll(title, progress);

        Scene scene = new Scene(loadingBox, 400, 300);
        stage.setTitle("National Gallery Route Finder");
        stage.setScene(scene);
        stage.show();
    }

    private void showMainWindow(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1e1e1e;");

        root.setLeft(createControlPanel());

        mapCanvas = new MapCanvas(graph);

        ScrollPane mapScroll = new ScrollPane(mapCanvas);
        mapScroll.setPannable(true);
        mapScroll.setFitToWidth(false);
        mapScroll.setFitToHeight(false);

        root.setCenter(mapScroll);

        routeInfoArea = new TextArea();
        routeInfoArea.setEditable(false);
        routeInfoArea.setPrefHeight(180);
        routeInfoArea.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: #e0e0e0;");

        statusLabel = new Label("Loading...");
        statusLabel.setStyle("-fx-text-fill: #aaa;");

        VBox bottomBox = new VBox(routeInfoArea, statusLabel);
        root.setBottom(bottomBox);

        Scene scene = new Scene(root, 1400, 850);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: #2d2d2d;");
        panel.setPrefWidth(350);

        // Start
        Label startLabel = new Label("📍 Starting Point:");
        startLabel.setTextFill(Color.WHITE);
        startCombo = new ComboBox<>();
        startCombo.getItems().addAll(roomDisplayNames);
        startCombo.setPromptText("Select start room");

        // End
        Label endLabel = new Label("📍 Destination:");
        endLabel.setTextFill(Color.WHITE);
        endCombo = new ComboBox<>();
        endCombo.getItems().addAll(roomDisplayNames);
        endCombo.setPromptText("Select destination");

        // Waypoints
        Label wpLabel = new Label("📍 Waypoints (must visit):");
        wpLabel.setTextFill(Color.ORANGE);
        waypointsList = new ListView<>();
        waypointsList.setPrefHeight(80);

        Button addWp = new Button("+ Add Waypoint");
        addWp.setOnAction(e -> addWaypoint());
        Button removeWp = new Button("- Remove");
        removeWp.setOnAction(e -> removeWaypoint());
        HBox wpBtns = new HBox(5, addWp, removeWp);

        // Avoid
        Label avoidLabel = new Label("🚫 Rooms to Avoid:");
        avoidLabel.setTextFill(Color.RED);
        avoidList = new ListView<>();
        avoidList.setPrefHeight(80);

        Button addAvoid = new Button("+ Add to Avoid");
        addAvoid.setOnAction(e -> addAvoidRoom());
        Button removeAvoid = new Button("- Remove");
        removeAvoid.setOnAction(e -> removeAvoidRoom());
        HBox avoidBtns = new HBox(5, addAvoid, removeAvoid);

        // Artists
        Label artistLabel = new Label("🎨 Favorite Artists:");
        artistLabel.setTextFill(Color.PURPLE);
        artistField = new TextField();
        artistField.setPromptText("e.g., Rembrandt, Van Gogh");
        Button setArtists = new Button("Set Artists");
        setArtists.setOnAction(e -> setFavoriteArtists());

        // Buttons
        Button anyRouteBtn = new Button("🔍 Find Any Route");
        anyRouteBtn.setOnAction(e -> findAnyRoute());
        Button multiRouteBtn = new Button("🔍 Multiple Routes (DFS)");
        multiRouteBtn.setOnAction(e -> findMultipleRoutes());
        Button dijkstraBtn = new Button("⚡ Shortest (Dijkstra)");
        dijkstraBtn.setOnAction(e -> findDijkstraRoute());
        Button bfsBtn = new Button("🌲 Shortest (BFS Graph)");
        bfsBtn.setOnAction(e -> findBFSRoute());
        Button interestingBtn = new Button("⭐ Most Interesting");
        interestingBtn.setOnAction(e -> findInterestingRoute());
        Button clearBtn = new Button("🗑️ Clear Map");
        clearBtn.setOnAction(e -> clearMap());

        // Results
        Label resultsLabel = new Label("📋 Found Routes:");
        resultsLabel.setTextFill(Color.CYAN);
        routesListView = new ListView<>();
        routesListView.setPrefHeight(120);
        routesListView.setOnMouseClicked(e -> {
            int idx = routesListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < foundRoutes.size()) {
                displayRoute(foundRoutes.get(idx));
            }
        });

        panel.getChildren().addAll(
                startLabel, startCombo,
                endLabel, endCombo,
                new Separator(),
                wpLabel, waypointsList, wpBtns,
                new Separator(),
                avoidLabel, avoidList, avoidBtns,
                new Separator(),
                artistLabel, artistField, setArtists,
                new Separator(),
                anyRouteBtn, multiRouteBtn, dijkstraBtn, bfsBtn, interestingBtn, clearBtn,
                new Separator(),
                resultsLabel, routesListView
        );

        return panel;
    }

    private void addWaypoint() {
        String selected = startCombo.getValue();
        if (selected != null && !waypointsList.getItems().contains(selected)) {
            waypointsList.getItems().add(selected);
            waypoints.add(displayToNode.get(selected));
        }
    }

    private void removeWaypoint() {
        int idx = waypointsList.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            waypointsList.getItems().remove(idx);
            waypoints.remove(idx);
        }
    }

    private void addAvoidRoom() {
        String selected = startCombo.getValue();
        if (selected != null && !avoidList.getItems().contains(selected)) {
            avoidList.getItems().add(selected);
            avoidRooms.add(displayToNode.get(selected).getRoom().getId());
        }
    }

    private void removeAvoidRoom() {
        int idx = avoidList.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            String item = avoidList.getItems().get(idx);
            avoidList.getItems().remove(idx);
            GraphNode node = displayToNode.get(item);
            if (node != null) avoidRooms.remove(node.getRoom().getId());
        }
    }

    private void setFavoriteArtists() {
        String text = artistField.getText();
        favoriteArtists.clear();
        for (String artist : text.split(",")) {
            String trimmed = artist.trim().toLowerCase();
            if (!trimmed.isEmpty()) favoriteArtists.add(trimmed);
        }
        showAlert("Favorite artists set: " + favoriteArtists);
    }

    private GraphNode getStart() {
        return startCombo.getValue() != null ? displayToNode.get(startCombo.getValue()) : null;
    }

    private GraphNode getEnd() {
        return endCombo.getValue() != null ? displayToNode.get(endCombo.getValue()) : null;
    }

    private boolean validateSelections() {
        return getStart() != null && getEnd() != null;
    }

    private void findAnyRoute() {
        if (!validateSelections()) return;
        statusLabel.setText("Finding any route...");
        List<GraphNode> route = routeFinder.findAnyRoute(graph, getStart(), getEnd(), avoidRooms, waypoints);
        displayResult(route, "Any Route");
    }

    private void findMultipleRoutes() {
        if (!validateSelections()) return;
        TextInputDialog dialog = new TextInputDialog("5");
        dialog.setTitle("Max Routes");
        int maxRoutes = dialog.showAndWait().map(Integer::parseInt).orElse(5);
        maxRoutes = Math.min(maxRoutes, 20);

        statusLabel.setText("Finding multiple routes...");
        foundRoutes = routeFinder.findMultipleRoutes(graph, getStart(), getEnd(), maxRoutes, avoidRooms, waypoints);

        routesListView.getItems().clear();
        for (int i = 0; i < foundRoutes.size(); i++) {
            double dist = routeFinder.calculateRouteDistance(foundRoutes.get(i));
            routesListView.getItems().add(String.format("Route %d: %.0f units", i+1, dist));
        }
        if (!foundRoutes.isEmpty()) displayRoute(foundRoutes.get(0));
    }

    private void findDijkstraRoute() {
        if (!validateSelections()) return;
        statusLabel.setText("Running Dijkstra...");
        List<GraphNode> route = routeFinder.dijkstraShortestPath(graph, getStart(), getEnd(), avoidRooms, waypoints);
        displayResult(route, "Dijkstra");
    }

    private void findBFSRoute() {
        if (!validateSelections()) return;
        statusLabel.setText("Running BFS...");
        List<GraphNode> route = routeFinder.bfsShortestPath(graph, getStart(), getEnd(), avoidRooms, waypoints);
        displayResult(route, "BFS");
    }

    private void findInterestingRoute() {
        if (!validateSelections()) return;
        if (favoriteArtists.isEmpty()) {
            showAlert("Set favorite artists first!");
            return;
        }
        statusLabel.setText("Finding interesting route...");
        List<GraphNode> route = routeFinder.mostInterestingRoute(graph, getStart(), getEnd(), favoriteArtists, avoidRooms, waypoints);
        displayResult(route, "Interesting");
    }

    private void clearMap() {
        mapCanvas.clearRoutes();
        routeInfoArea.clear();
        statusLabel.setText("Map cleared");
    }

    private void displayResult(List<GraphNode> route, String title) {
        if (route == null || route.isEmpty()) {
            showAlert("No route found!");
            return;
        }
        foundRoutes = List.of(route);
        displayRoute(route);
        double dist = routeFinder.calculateRouteDistance(route);
        statusLabel.setText(title + " | " + route.size() + " rooms | " + String.format("%.0f", dist) + " units");
    }

    private void displayRoute(List<GraphNode> route) {
        mapCanvas.setAvoidRooms(avoidRooms);
        mapCanvas.displayRoute(route);

        StringBuilder info = new StringBuilder();
        info.append("Route: ").append(route.size()).append(" rooms\n");
        info.append("Distance: ").append(String.format("%.0f", routeFinder.calculateRouteDistance(route))).append(" units\n\n");

        for (int i = 0; i < route.size(); i++) {
            GraphNode node = route.get(i);
            info.append(String.format("%d. %s\n", i+1, node.getRoom().getName()));
            for (var art : node.getRoom().getArtworks()) {
                String star = favoriteArtists.contains(art.getArtist().toLowerCase()) ? " ★" : "";
                info.append(String.format("   🖼️ %s by %s%s\n", art.getTitle(), art.getArtist(), star));
            }
        }
        routeInfoArea.setText(info.toString());
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(Stage stage, String error) {
        Label errorLabel = new Label("Error: " + error);
        errorLabel.setTextFill(Color.RED);
        stage.getScene().setRoot(errorLabel);
    }
}


