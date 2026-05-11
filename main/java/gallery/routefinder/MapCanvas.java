package gallery.routefinder;

import gallery.routefinder.graph.Graph;
import gallery.routefinder.graph.GraphNode;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;

// draws the gallery map and all route markers
public class MapCanvas extends Canvas {

    private Graph graph;
    private List<GraphNode> currentRoute;
    private Set<String> avoidRoomIds = new HashSet<>();
    private List<GraphNode> waypoints = new ArrayList<>();
    private Image mapImage;
    private List<int[]> pixelPath = null;

    // pixel selection for BFS
    private boolean pixelSelectionMode = false;
    private BiConsumer<Integer, Integer> pixelSelectionCallback;
    private GraphNode selectedStart = null;
    private GraphNode selectedEnd = null;
    private boolean showDebugNodes = false;

    public MapCanvas(Graph graph) {
        this.graph = graph;
        // load map image
        InputStream stream = getClass().getResourceAsStream("/level2.png");

        if (stream == null) {
            System.out.println("ERROR: level2.png not found in resources!");
            setWidth(1000);
            setHeight(700);
        } else {
            mapImage = new Image(stream);
            setWidth(mapImage.getWidth());
            setHeight(mapImage.getHeight());
        }

        // handle mouse clicks for pixel bfs selection
        setOnMouseClicked(e -> {
            if (pixelSelectionMode && pixelSelectionCallback != null) {
                int x = (int) e.getX();
                int y = (int) e.getY();
                pixelSelectionCallback.accept(x, y);
            } else {
                System.out.println("Clicked at: x=" + (int) e.getX() + ", y=" + (int) e.getY());
            }
        });

        drawMap();
    }

    // no scaling coordinates from csv match image pixels directly
    private double mapX(int x) {
        return x;
    }

    private double mapY(int y) {
        return y;
    }

    public Image getMapImage() {
        return mapImage;
    }

    // highlights selected start and end rooms on map
    public void setSelectedPoints(GraphNode start, GraphNode end) {
        this.selectedStart = start;
        this.selectedEnd = end;
        drawMap();
    }

    // enables pixel selection mode for pixel bfs
    public void setPixelSelectionMode(boolean enabled, BiConsumer<Integer, Integer> callback) {
        this.pixelSelectionMode = enabled;
        this.pixelSelectionCallback = callback;
        if (enabled) {
            setStyle("-fx-cursor: crosshair;");
        } else {
            setStyle("-fx-cursor: default;");
        }
    }

    // draws map image then all markers
    public void drawMap() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        // draw background map
        if (mapImage != null) {
            gc.drawImage(mapImage, 0, 0);
        } else {
            gc.setFill(Color.rgb(240, 240, 245));
            gc.fillRect(0, 0, getWidth(), getHeight());
        }

        // showDebugNodes = true
        if (showDebugNodes) {
            drawNormalNodes(gc);
        }

        drawAvoidRooms(gc);      // red X marks
        drawSelectedPoints(gc);  // S and D markers
        drawWaypoints(gc);       // W markers

        if (pixelPath != null && !pixelPath.isEmpty()) {
            drawPixelPath(gc);
        }

        // roure (red)
        if (currentRoute != null && currentRoute.size() > 1) {
            drawRoute(gc);
        }
    }

    // draws start and end markers
    private void drawSelectedPoints(GraphicsContext gc) {
        if (selectedStart != null) {
            double x = mapX(selectedStart.getRoom().getX());
            double y = mapY(selectedStart.getRoom().getY());
            drawMarker(gc, x, y, "S", Color.LIMEGREEN, 11);
        }
        if (selectedEnd != null) {
            double x = mapX(selectedEnd.getRoom().getX());
            double y = mapY(selectedEnd.getRoom().getY());
            drawMarker(gc, x, y, "D", Color.RED, 11);
        }
    }

    // draws waypoint markers
    private void drawWaypoints(GraphicsContext gc) {
        if (waypoints == null) return;
        for (GraphNode wp : waypoints) {
            double x = mapX(wp.getRoom().getX());
            double y = mapY(wp.getRoom().getY());
            drawMarker(gc, x, y, "W", Color.ORANGE, 11);
        }
    }

    //all room nodes (debug mode only)
    private void drawNormalNodes(GraphicsContext gc) {
        for (GraphNode node : graph.getAllNodes()) {
            double x = mapX(node.getRoom().getX());
            double y = mapY(node.getRoom().getY());

            gc.setGlobalAlpha(0.65);
            gc.setFill(Color.rgb(40, 130, 210));
            gc.fillOval(x - 5, y - 5, 10, 10);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1);
            gc.strokeOval(x - 5, y - 5, 10, 10);
            gc.setGlobalAlpha(1.0);

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", 12));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(node.getRoom().getId(), x, y + 2);
        }
    }

    // draws red X marks on rooms to avoid
    private void drawAvoidRooms(GraphicsContext gc) {
        for (String roomId : avoidRoomIds) {
            GraphNode node = graph.getNodeById(roomId);
            if (node == null) continue;

            double x = mapX(node.getRoom().getX());
            double y = mapY(node.getRoom().getY());

            gc.setFill(Color.rgb(80, 80, 80, 0.8));
            gc.fillOval(x - 10, y - 10, 20, 20);
            gc.setStroke(Color.RED);
            gc.setLineWidth(3);
            gc.strokeLine(x - 7, y - 7, x + 7, y + 7);
            gc.strokeLine(x + 7, y - 7, x - 7, y + 7);
        }
    }

    // draws the main route as red line with numbered markers
    private void drawRoute(GraphicsContext gc) {
        if (currentRoute == null || currentRoute.size() < 2) return;

        gc.setStroke(Color.rgb(255, 30, 30, 0.9));
        gc.setLineWidth(5);

        for (int i = 0; i < currentRoute.size() - 1; i++) {
            GraphNode curr = currentRoute.get(i);
            GraphNode next = currentRoute.get(i + 1);

            double x1 = mapX(curr.getRoom().getX());
            double y1 = mapY(curr.getRoom().getY());
            double x2 = mapX(next.getRoom().getX());
            double y2 = mapY(next.getRoom().getY());

            gc.strokeLine(x1, y1, x2, y2);
        }

        for (int i = 0; i < currentRoute.size(); i++) {
            GraphNode node = currentRoute.get(i);
            double x = mapX(node.getRoom().getX());
            double y = mapY(node.getRoom().getY());

            if (i == 0) {
                drawMarker(gc, x, y, "S", Color.LIMEGREEN, 13);
            } else if (i == currentRoute.size() - 1) {
                drawMarker(gc, x, y, "D", Color.RED, 13);
            } else if (waypoints != null && waypoints.contains(node)) {
                drawMarker(gc, x, y, "W", Color.ORANGE, 11);
            } else {
                drawMarker(gc, x, y, String.valueOf(i), Color.ORANGE, 10);
            }
        }
    }

    // draws pixel bfs path (blue line)
    private void drawPixelPath(GraphicsContext gc) {
        if (pixelPath == null || pixelPath.size() < 2) return;

        gc.setStroke(Color.rgb(0, 100, 255, 0.8));
        gc.setLineWidth(2);

        for (int i = 0; i < pixelPath.size() - 1; i++) {
            int[] p1 = pixelPath.get(i);
            int[] p2 = pixelPath.get(i + 1);
            gc.strokeLine(p1[0], p1[1], p2[0], p2[1]);
        }

        int[] start = pixelPath.get(0);
        int[] end = pixelPath.get(pixelPath.size() - 1);
        drawPixelMarker(gc, start[0], start[1], "S", Color.LIMEGREEN);
        drawPixelMarker(gc, end[0], end[1], "D", Color.RED);
    }

    // helper to draw pixel bfs markers
    private void drawPixelMarker(GraphicsContext gc, double x, double y, String text, Color color) {
        gc.setFill(color);
        gc.fillOval(x - 8, y - 8, 16, 16);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        gc.strokeOval(x - 8, y - 8, 16, 16);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 9));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(text, x, y + 3);
    }

    // generic marker drawer
    private void drawMarker(GraphicsContext gc, double x, double y, String text, Color color, double radius) {
        gc.setFill(color);
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", 10));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(text, x, y + 3);
    }

    // shows route on map
    public void displayRoute(List<GraphNode> route) {
        this.currentRoute = route;
        this.pixelPath = null;
        drawMap();
    }

    // shows pixel bfs path on map
    public void displayPixelPath(List<int[]> path) {
        this.pixelPath = path;
        this.currentRoute = null;
        drawMap();
    }

    // clears all routes from map
    public void clearRoutes() {
        this.currentRoute = null;
        this.pixelPath = null;
        drawMap();
    }

    // updates avoid rooms and redraws
    public void setAvoidRooms(Set<String> avoidRoomIds) {
        this.avoidRoomIds = avoidRoomIds != null ? avoidRoomIds : new HashSet<>();
        drawMap();
    }

    // updates waypoints and redraws
    public void setWaypoints(List<GraphNode> waypoints) {
        this.waypoints = waypoints != null ? waypoints : new ArrayList<>();
        drawMap();
    }
}