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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.scene.control.Alert;

public class MapCanvas extends Canvas {

    private Graph graph;
    private List<GraphNode> currentRoute;
    private Set<String> avoidRoomIds = new HashSet<>();

    private Image mapImage;

    public MapCanvas(Graph graph) {
        this.graph = graph;

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



        setOnMouseClicked(e -> {
            int x = (int) e.getX();
            int y = (int) e.getY();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Map coordinates");
            alert.setHeaderText("Clicked coordinates");
            alert.setContentText("x=" + x + "; y=" + y);
            alert.showAndWait();
        });


        drawMap();
    }

    private double mapX(int x) {
        return x;
    }

    private double mapY(int y) {
        return y;
    }

    public void drawMap() {
        GraphicsContext gc = getGraphicsContext2D();

        gc.clearRect(0, 0, getWidth(), getHeight());

        if (mapImage != null) {
            gc.drawImage(mapImage, 0, 0);
        } else {
            gc.setFill(Color.rgb(240, 240, 245));
            gc.fillRect(0, 0, getWidth(), getHeight());
        }

        drawNormalNodes(gc);
        drawAvoidRooms(gc);

        if (currentRoute != null && currentRoute.size() > 1) {
            drawRoute(gc);
        }
    }

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
            gc.setFont(Font.font("Arial", 6));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(node.getRoom().getId(), x, y + 2);
        }
    }

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
                drawMarker(gc, x, y, "E", Color.RED, 13);
            } else {
                drawMarker(gc, x, y, String.valueOf(i), Color.ORANGE, 10);
            }
        }
    }

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

    public void displayRoute(List<GraphNode> route) {
        this.currentRoute = route;
        drawMap();
    }

    public void clearRoutes() {
        this.currentRoute = null;
        drawMap();
    }

    public void setAvoidRooms(Set<String> avoidRoomIds) {
        this.avoidRoomIds = avoidRoomIds != null ? avoidRoomIds : new HashSet<>();
        drawMap();
    }
}