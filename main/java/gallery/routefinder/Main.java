package gallery.routefinder;

import gallery.routefinder.algorithm.RouteFinder;
import gallery.routefinder.db.DataLoader;
import gallery.routefinder.graph.Graph;
import gallery.routefinder.graph.GraphNode;
import gallery.routefinder.model.Artwork;
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
    private ComboBox<String> waypointCombo;
    private ComboBox<String> avoidCombo;
    private ListView<String> waypointsListView;
    private ListView<String> avoidListView;
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

    // Pixel BFS
    private boolean selectingPixelStart = false;
    private int[] pixelStart = null;

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
            Graph graph = DataLoader.loadFromCSV("rooms.csv", "connections.csv", "artworks.csv");
            System.out.println("Loaded rooms: " + graph.getAllNodes().size());
            for (GraphNode node : graph.getAllNodes()) {
                System.out.println("  " + node.getRoom().getName());
                for (Artwork artwork : node.getRoom().getArtworks()) {
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
        graph = DataLoader.loadFromCSV("rooms.csv", "connections.csv", "artworks.csv");

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

        // Left panel with ScrollPane
        ScrollPane leftScrollPane = new ScrollPane(createControlPanel());
        leftScrollPane.setFitToWidth(true);
        leftScrollPane.setFitToHeight(false);
        leftScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        leftScrollPane.setStyle("-fx-background-color: #2d2d2d; -fx-background: #2d2d2d;");
        leftScrollPane.setPrefWidth(380);
        root.setLeft(leftScrollPane);

        mapCanvas = new MapCanvas(graph);
        ScrollPane mapScroll = new ScrollPane(mapCanvas);
        mapScroll.setPannable(true);
        mapScroll.setFitToWidth(false);
        mapScroll.setFitToHeight(false);
        root.setCenter(mapScroll);

        routeInfoArea = new TextArea();
        routeInfoArea.setEditable(false);
        routeInfoArea.setPrefHeight(200);
        routeInfoArea.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: #e0e0e0;");

        statusLabel = new Label("Loading...");
        statusLabel.setStyle("-fx-text-fill: #aaa; -fx-padding: 5;");

        VBox bottomBox = new VBox(routeInfoArea, statusLabel);
        root.setBottom(bottomBox);

        Scene scene = new Scene(root, 1200, 750);
        stage.setScene(scene);
        stage.setMaximized(false);
        stage.centerOnScreen();
        stage.show();
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: #2d2d2d;");

        // Start room
        Label startLabel = new Label("📍 Starting Point:");
        startLabel.setTextFill(Color.WHITE);
        startCombo = new ComboBox<>();
        startCombo.getItems().addAll(roomDisplayNames);
        startCombo.setPromptText("Select start room");
        startCombo.setOnAction(e -> updateSelectedPoints());

        // Destination room
        Label endLabel = new Label("📍 Destination:");
        endLabel.setTextFill(Color.WHITE);
        endCombo = new ComboBox<>();
        endCombo.getItems().addAll(roomDisplayNames);
        endCombo.setPromptText("Select destination");
        endCombo.setOnAction(e -> updateSelectedPoints());

        // Waypoint selection
        Label wpLabel = new Label("📍 Add Waypoint:");
        wpLabel.setTextFill(Color.ORANGE);
        waypointCombo = new ComboBox<>();
        waypointCombo.getItems().addAll(roomDisplayNames);
        waypointCombo.setPromptText("Select room as waypoint");

        waypointsListView = new ListView<>();
        waypointsListView.setPrefHeight(80);

        Button addWp = new Button("+ Add Waypoint");
        addWp.setOnAction(e -> addWaypoint());
        Button removeWp = new Button("- Remove");
        removeWp.setOnAction(e -> removeWaypoint());
        HBox wpBtns = new HBox(5, addWp, removeWp);

        // Avoid room selection
        Label avoidLabel = new Label("🚫 Add Room to Avoid:");
        avoidLabel.setTextFill(Color.RED);
        avoidCombo = new ComboBox<>();
        avoidCombo.getItems().addAll(roomDisplayNames);
        avoidCombo.setPromptText("Select room to avoid");

        avoidListView = new ListView<>();
        avoidListView.setPrefHeight(80);

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
        Button anyRouteBtn = new Button("Find Any Route");
        anyRouteBtn.setOnAction(e -> findAnyRoute());
        Button multiRouteBtn = new Button("Multiple Routes (DFS)");
        multiRouteBtn.setOnAction(e -> findMultipleRoutes());
        Button dijkstraBtn = new Button("Shortest Path (Dijkstra)");
        dijkstraBtn.setOnAction(e -> findDijkstraRoute());
        Button bfsBtn = new Button("BFS Graph Route");
        bfsBtn.setOnAction(e -> findBFSRoute());
        Button interestingBtn = new Button("Most Interesting Route ⭐");
        interestingBtn.setOnAction(e -> findInterestingRoute());
        Button pixelBfsBtn = new Button("Pixel BFS (Click on Map)");
        pixelBfsBtn.setOnAction(e -> startPixelSelection());
        Button clearBtn = new Button("🗑 Clear Map");
        clearBtn.setOnAction(e -> clearMap());

        // Results list
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
                wpLabel, waypointCombo, waypointsListView, wpBtns,
                new Separator(),
                avoidLabel, avoidCombo, avoidListView, avoidBtns,
                new Separator(),
                artistLabel, artistField, setArtists,
                new Separator(),
                anyRouteBtn, multiRouteBtn, dijkstraBtn, bfsBtn, interestingBtn, pixelBfsBtn, clearBtn,
                new Separator(),
                resultsLabel, routesListView
        );

        return panel;
    }

    private void updateSelectedPoints() {
        GraphNode start = getStart();
        GraphNode end = getEnd();
        mapCanvas.setSelectedPoints(start, end);
    }

    private void addWaypoint() {
        String selected = waypointCombo.getValue();
        if (selected != null) {
            waypointsListView.getItems().add(selected);
            waypoints.add(displayToNode.get(selected));
            waypointCombo.setValue(null);
            mapCanvas.setWaypoints(waypoints);
        } else {
            showAlert("Select a room from the waypoint dropdown");
        }
    }

    private void removeWaypoint() {
        int idx = waypointsListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            waypointsListView.getItems().remove(idx);
            waypoints.remove(idx);
            mapCanvas.setWaypoints(waypoints);
        }
    }

    private void addAvoidRoom() {
        String selected = avoidCombo.getValue();
        if (selected != null) {
            avoidListView.getItems().add(selected);
            avoidRooms.add(displayToNode.get(selected).getRoom().getId());
            avoidCombo.setValue(null);
            mapCanvas.setAvoidRooms(avoidRooms);
        } else {
            showAlert("Select a room from the avoid dropdown");
        }
    }

    private void removeAvoidRoom() {
        int idx = avoidListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            String item = avoidListView.getItems().get(idx);
            avoidListView.getItems().remove(idx);
            GraphNode node = displayToNode.get(item);
            if (node != null) avoidRooms.remove(node.getRoom().getId());
            mapCanvas.setAvoidRooms(avoidRooms);
        }
    }

    private void setFavoriteArtists() {
        String text = artistField.getText();
        favoriteArtists.clear();
        StringBuilder msg = new StringBuilder("Favorite artists set:\n");
        for (String artist : text.split(",")) {
            String trimmed = artist.trim();
            if (!trimmed.isEmpty()) {
                favoriteArtists.add(trimmed.toLowerCase());
                msg.append("  - ").append(trimmed).append("\n");
            }
        }
        msg.append("\nNow click \"Most Interesting Route\"");
        routeInfoArea.setText(msg.toString());
        statusLabel.setText("Artists set: " + favoriteArtists);
        showAlert(msg.toString());
    }

    private void startPixelSelection() {
        selectingPixelStart = true;
        pixelStart = null;
        statusLabel.setText("Click on map to select START point for pixel BFS");
        mapCanvas.setPixelSelectionMode(true, (x, y) -> Platform.runLater(() -> onPixelSelected(x, y)));
    }

    private void onPixelSelected(int x, int y) {
        if (selectingPixelStart) {
            pixelStart = new int[]{x, y};
            selectingPixelStart = false;
            statusLabel.setText("Now click on map to select END point");
        } else if (pixelStart != null) {
            int[] pixelEnd = new int[]{x, y};
            selectingPixelStart = false;
            mapCanvas.setPixelSelectionMode(false, null);

            statusLabel.setText("Running Pixel BFS...");

            new Thread(() -> {
                try {
                    PixelBFS pixelBFS = new PixelBFS("/level2bw.png");
                    PixelBFS.PixelPathResult result = pixelBFS.findShortestPath(
                            pixelStart[0], pixelStart[1], pixelEnd[0], pixelEnd[1]
                    );

                    Platform.runLater(() -> {
                        if (result.path != null && !result.path.isEmpty()) {
                            mapCanvas.displayPixelPath(result.path);
                            routeInfoArea.setText(String.format(
                                    "═══════════════════════════════════════════════════════════════\n" +
                                            "  PIXEL BFS PATH FINDING\n" +
                                            "═══════════════════════════════════════════════════════════════\n\n" +
                                            "  Start point: (%d, %d)\n" +
                                            "  End point: (%d, %d)\n" +
                                            "  Steps: %d\n" +
                                            "  Path points: %d\n\n" +
                                            "  NOTE: This path follows walkable areas (white pixels)\n" +
                                            "  on the black-and-white map image.\n" +
                                            "  Distance is measured in pixel steps.",
                                    pixelStart[0], pixelStart[1], pixelEnd[0], pixelEnd[1],
                                    result.distance, result.path.size()
                            ));
                            statusLabel.setText(String.format("Pixel BFS complete! Distance: %d steps", result.distance));
                        } else {
                            showAlert("No walkable path found between selected points!");
                            statusLabel.setText("No pixel path found");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showAlert("Pixel BFS error: " + e.getMessage());
                        statusLabel.setText("Pixel BFS error");
                    });
                }
            }).start();

            pixelStart = null;
        }
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

    private void findAnyRoute() {
        if (!validateSelections()) return;
        statusLabel.setText("Finding any route...");
        GraphNode start = getStart();
        GraphNode end = getEnd();

        new Thread(() -> {
            try {
                List<GraphNode> route = RouteFinder.findSingleRoute(start, end, avoidRooms, waypoints);
                Platform.runLater(() -> displayResult(route, "Any Route (DFS)"));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Error: " + e.getMessage());
                    statusLabel.setText("Error finding route");
                });
            }
        }).start();
    }

    private void findMultipleRoutes() {
        if (!validateSelections()) return;
        TextInputDialog dialog = new TextInputDialog("5");
        dialog.setTitle("Max Routes");
        dialog.setHeaderText("Maximum number of routes to find");
        dialog.setContentText("Enter max routes (1-10):");

        Optional<String> result = dialog.showAndWait();
        int maxRoutes = 5;
        if (result.isPresent()) {
            try {
                maxRoutes = Integer.parseInt(result.get());
                maxRoutes = Math.min(maxRoutes, 10);
            } catch (NumberFormatException ignored) {}
        }

        statusLabel.setText("Finding multiple routes...");
        GraphNode start = getStart();
        GraphNode end = getEnd();

        new Thread(() -> {
            try {
                foundRoutes = RouteFinder.findMultipleRoutes(start, end, maxRoutes, avoidRooms, waypoints, null);

                Platform.runLater(() -> {
                    if (foundRoutes == null || foundRoutes.isEmpty()) {
                        showAlert("No routes found!");
                        statusLabel.setText("No routes found");
                        return;
                    }

                    routesListView.getItems().clear();
                    for (int i = 0; i < foundRoutes.size(); i++) {
                        double dist = RouteFinder.calculateRouteDistance(foundRoutes.get(i));
                        routesListView.getItems().add(String.format("Route %d: %.0f units, %d rooms", i+1, dist, foundRoutes.get(i).size()));
                    }
                    if (!foundRoutes.isEmpty()) displayRoute(foundRoutes.get(0));
                    statusLabel.setText("Found " + foundRoutes.size() + " routes");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Error: " + e.getMessage());
                    statusLabel.setText("Error finding routes");
                });
            }
        }).start();
    }

    private void findDijkstraRoute() {
        if (!validateSelections()) return;
        statusLabel.setText("Running Dijkstra...");
        GraphNode start = getStart();
        GraphNode end = getEnd();

        new Thread(() -> {
            try {
                List<GraphNode> route = RouteFinder.dijkstraShortestPath(start, end, avoidRooms, waypoints);
                Platform.runLater(() -> displayResult(route, "Dijkstra"));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Error: " + e.getMessage());
                    statusLabel.setText("Error running Dijkstra");
                });
            }
        }).start();
    }

    private void findBFSRoute() {
        if (!validateSelections()) return;
        statusLabel.setText("Running BFS Graph...");
        GraphNode start = getStart();
        GraphNode end = getEnd();

        new Thread(() -> {
            try {
                List<GraphNode> route = RouteFinder.bfsShortestPath(start, end, avoidRooms, waypoints);
                Platform.runLater(() -> displayResult(route, "BFS Graph"));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Error: " + e.getMessage());
                    statusLabel.setText("Error running BFS");
                });
            }
        }).start();
    }

    private void findInterestingRoute() {
        if (!validateSelections()) return;
        if (favoriteArtists.isEmpty()) {
            showAlert("Set favorite artists first!");
            return;
        }
        statusLabel.setText("Finding interesting route...");
        GraphNode start = getStart();
        GraphNode end = getEnd();

        new Thread(() -> {
            try {
                List<GraphNode> route = RouteFinder.mostInterestingRoute(graph, start, end, favoriteArtists, avoidRooms, waypoints);
                Platform.runLater(() -> displayResult(route, "Interesting"));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Error: " + e.getMessage());
                    statusLabel.setText("Error finding interesting route");
                });
            }
        }).start();
    }

    private void clearMap() {
        if (mapCanvas != null) {
            mapCanvas.clearRoutes();
            mapCanvas.setWaypoints(waypoints);
            mapCanvas.setAvoidRooms(avoidRooms);
            updateSelectedPoints();
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
            mapCanvas.setWaypoints(waypoints);
            mapCanvas.displayRoute(route);
        }

        StringBuilder info = new StringBuilder();
        info.append("═══════════════════════════════════════════════════════════════\n");
        info.append(String.format("  ROUTE: %d rooms  |  Distance: %.0f units\n",
                route.size(), RouteFinder.calculateRouteDistance(route)));
        info.append("═══════════════════════════════════════════════════════════════\n\n");

        for (int i = 0; i < route.size(); i++) {
            GraphNode node = route.get(i);
            boolean isWaypoint = waypoints.contains(node);
            info.append(String.format("%2d. %s %s\n", i + 1, node.getRoom().getName(), isWaypoint ? "📍 WAYPOINT" : ""));
            info.append(String.format("     [%s]\n", node.getRoom().getId()));

            List<Artwork> artworks = node.getRoom().getArtworks();
            if (artworks.isEmpty()) {
                info.append("     (No artworks in this room)\n");
            } else {
                for (Artwork art : artworks) {
                    boolean isFavorite = favoriteArtists.stream().anyMatch(a -> art.getArtist().toLowerCase().contains(a));
                    info.append(String.format("        🖼️ %s by %s%s\n",
                            art.getTitle(), art.getArtist(), isFavorite ? " ★" : ""));
                }
            }
            info.append("\n");
        }

        info.append("═══════════════════════════════════════════════════════════════\n");
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