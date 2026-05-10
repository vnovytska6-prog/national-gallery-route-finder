package gallery.routefinder;

import gallery.routefinder.algorithm.RouteFinder;
import gallery.routefinder.db.DataLoader;
import gallery.routefinder.graph.Graph;
import gallery.routefinder.graph.GraphNode;
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

public class Main extends Application {

    private Graph graph;
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
        if (args.length > 0 && args[0].equals("console")) {
            runConsoleMode();
        } else {
            launch(args);
        }
    }

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
                System.out.println("  " + node.getRoom().getName());
                for (var artwork : node.getRoom().getArtworks()) {
                    System.out.println("    🖼️ " + artwork);
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage primaryStage) {
        showLoadingScreen(primaryStage);

        new Thread(() -> {
            try {
                loadData();
                Platform.runLater(() -> {
                    showMainWindow(primaryStage);
                    statusLabel.setText("Ready | " + graph.getAllNodes().size() + " rooms loaded");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    showError(primaryStage, e.getMessage());
                });
            }
        }).start();
    }

    private void loadData() throws Exception {
        graph = DataLoader.loadFromCSV(
                "rooms.csv",
                "connections.csv",
                "artworks.csv"
        );

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
        mapScroll.setFitToWidth(true);
        mapScroll.setFitToHeight(true);

        root.setCenter(mapScroll);

        routeInfoArea = new TextArea();
        routeInfoArea.setEditable(false);
        routeInfoArea.setPrefHeight(180);
        routeInfoArea.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: #e0e0e0;");

        statusLabel = new Label("Loading...");
        statusLabel.setStyle("-fx-text-fill: #aaa; -fx-padding: 5;");

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

        Label startLabel = new Label("📍 Starting Point:");
        startLabel.setTextFill(Color.WHITE);
        startCombo = new ComboBox<>();
        startCombo.getItems().addAll(roomDisplayNames);
        startCombo.setPromptText("Select start room");

        Label endLabel = new Label("📍 Destination:");
        endLabel.setTextFill(Color.WHITE);
        endCombo = new ComboBox<>();
        endCombo.getItems().addAll(roomDisplayNames);
        endCombo.setPromptText("Select destination");

        Label wpLabel = new Label("📍 Waypoints (must visit):");
        wpLabel.setTextFill(Color.ORANGE);
        waypointsList = new ListView<>();
        waypointsList.setPrefHeight(80);

        Button addWp = new Button("+ Add Waypoint");
        addWp.setOnAction(e -> addWaypoint());
        Button removeWp = new Button("- Remove");
        removeWp.setOnAction(e -> removeWaypoint());
        HBox wpBtns = new HBox(5, addWp, removeWp);

        Label avoidLabel = new Label("🚫 Rooms to Avoid:");
        avoidLabel.setTextFill(Color.RED);
        avoidList = new ListView<>();
        avoidList.setPrefHeight(80);

        Button addAvoid = new Button("+ Add to Avoid");
        addAvoid.setOnAction(e -> addAvoidRoom());
        Button removeAvoid = new Button("- Remove");
        removeAvoid.setOnAction(e -> removeAvoidRoom());
        HBox avoidBtns = new HBox(5, addAvoid, removeAvoid);

        Label artistLabel = new Label("🎨 Favorite Artists:");
        artistLabel.setTextFill(Color.PURPLE);
        artistField = new TextField();
        artistField.setPromptText("e.g., Rembrandt, Van Gogh");
        Button setArtists = new Button("Set Artists");
        setArtists.setOnAction(e -> setFavoriteArtists());

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
        } else {
            showAlert("Select a start point first");
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
        if (getStart() == null) {
            showAlert("Please select a starting point!");
            return false;
        }
        if (getEnd() == null) {
            showAlert("Please select a destination!");
            return false;
        }
        return true;
    }

    // ========== ВЫЗОВЫ СТАТИЧЕСКИХ МЕТОДОВ ЧЕРЕЗ КЛАСС ==========

    private void findAnyRoute() {
        if (!validateSelections()) return;
        statusLabel.setText("Finding any route...");
        try {
            List<GraphNode> route = RouteFinder.findSingleRoute(getStart(), getEnd(), avoidRooms, waypoints);
            displayResult(route, "Any Route");
        } catch (Exception e) {
            showAlert("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void findMultipleRoutes() {
        if (!validateSelections()) return;
        TextInputDialog dialog = new TextInputDialog("5");
        dialog.setTitle("Max Routes");
        dialog.setHeaderText("Maximum number of routes to find");
        dialog.setContentText("Enter max routes (1-20):");

        Optional<String> result = dialog.showAndWait();
        int maxRoutes = 5;
        if (result.isPresent()) {
            try {
                maxRoutes = Integer.parseInt(result.get());
                maxRoutes = Math.min(maxRoutes, 20);
            } catch (NumberFormatException ignored) {}
        }

        statusLabel.setText("Finding multiple routes...");
        try {
            foundRoutes = RouteFinder.findMultipleRoutes(getStart(), getEnd(), maxRoutes, avoidRooms, waypoints, null);

            if (foundRoutes == null || foundRoutes.isEmpty()) {
                showAlert("No routes found!");
                return;
            }

            routesListView.getItems().clear();
            for (int i = 0; i < foundRoutes.size(); i++) {
                double dist = RouteFinder.calculateRouteDistance(foundRoutes.get(i));
                routesListView.getItems().add(String.format("Route %d: %.0f units, %d rooms", i+1, dist, foundRoutes.get(i).size()));
            }
            if (!foundRoutes.isEmpty()) displayRoute(foundRoutes.get(0));
            statusLabel.setText("Found " + foundRoutes.size() + " routes");
        } catch (Exception e) {
            showAlert("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void findDijkstraRoute() {
        if (!validateSelections()) return;
        statusLabel.setText("Running Dijkstra...");
        try {
            List<GraphNode> route = RouteFinder.dijkstraShortestPath(getStart(), getEnd(), avoidRooms, waypoints);
            displayResult(route, "Dijkstra");
        } catch (Exception e) {
            showAlert("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void findBFSRoute() {
        if (!validateSelections()) return;
        statusLabel.setText("Running BFS...");
        try {
            List<GraphNode> route = RouteFinder.bfsShortestPath(getStart(), getEnd(), avoidRooms, waypoints);
            displayResult(route, "BFS");
        } catch (Exception e) {
            showAlert("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void findInterestingRoute() {
        if (!validateSelections()) return;
        if (favoriteArtists.isEmpty()) {
            showAlert("Set favorite artists first!");
            return;
        }
        statusLabel.setText("Finding interesting route...");
        try {
            List<GraphNode> route = RouteFinder.mostInterestingRoute(graph, getStart(), getEnd(), favoriteArtists, avoidRooms, waypoints);
            displayResult(route, "Interesting");
        } catch (Exception e) {
            showAlert("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearMap() {
        if (mapCanvas != null) {
            mapCanvas.clearRoutes();
        }
        routeInfoArea.clear();
        routesListView.getItems().clear();
        foundRoutes.clear();
        statusLabel.setText("Map cleared");
    }

    private void displayResult(List<GraphNode> route, String title) {
        if (route == null || route.isEmpty()) {
            showAlert("No route found!");
            statusLabel.setText("No route found");
            return;
        }
        foundRoutes = new ArrayList<>();
        foundRoutes.add(route);
        displayRoute(route);
        double dist = RouteFinder.calculateRouteDistance(route);
        statusLabel.setText(title + " | " + route.size() + " rooms | " + String.format("%.0f", dist) + " units");
    }

    private void displayRoute(List<GraphNode> route) {
        if (mapCanvas != null) {
            mapCanvas.setAvoidRooms(avoidRooms);
            mapCanvas.displayRoute(route);
        }

        StringBuilder info = new StringBuilder();
        info.append("═══════════════════════════════════════════════════════════════\n");
        info.append("  Route: ").append(route.size()).append(" rooms\n");
        info.append("  Distance: ").append(String.format("%.0f", RouteFinder.calculateRouteDistance(route))).append(" units\n");
        info.append("═══════════════════════════════════════════════════════════════\n\n");

        for (int i = 0; i < route.size(); i++) {
            GraphNode node = route.get(i);
            info.append(String.format("%2d. %s\n", i + 1, node.getRoom().getName()));
            for (var art : node.getRoom().getArtworks()) {
                String star = favoriteArtists.contains(art.getArtist().toLowerCase()) ? " ★" : "";
                info.append(String.format("     🖼️ %s by %s%s\n", art.getTitle(), art.getArtist(), star));
            }
        }
        routeInfoArea.setText(info.toString());
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(Stage stage, String error) {
        Label errorLabel = new Label("Error: " + error);
        errorLabel.setTextFill(Color.RED);
        VBox root = new VBox(errorLabel);
        root.setAlignment(Pos.CENTER);
        Scene scene = new Scene(root, 400, 200);
        stage.setScene(scene);
    }
}