package gallery.routefinder;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import java.util.*;

/**
 * BFS Pixel-by-Pixel path finder for the gallery map.
 * This finds the shortest path between two points on the map image itself,
 * not using the room graph.
 */
public class PixelBFS {

    private Image mapImage;
    private boolean[][] walkable;
    private int width, height;

    public PixelBFS(Image mapImage) {
        this.mapImage = mapImage;
        analyzeMap();
    }

    /**
     * Analyzes the map to determine which pixels are walkable.
     * Walkable = white/light areas (rooms, corridors, doorways)
     */
    private void analyzeMap() {
        width = (int) mapImage.getWidth();
        height = (int) mapImage.getHeight();
        walkable = new boolean[width][height];

        PixelReader reader = mapImage.getPixelReader();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = reader.getArgb(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                // White/light gray = walkable (rooms, corridors)
                // Dark areas = walls, obstacles
                walkable[x][y] = (r > 200 && g > 200 && b > 200);
            }
        }
    }

    /**
     * Finds the shortest pixel path between start and end points using BFS.
     *
     * @param startX starting X coordinate on the map
     * @param startY starting Y coordinate on the map
     * @param endX destination X coordinate
     * @param endY destination Y coordinate
     * @return PixelPathResult containing the path and distance
     */
    public PixelPathResult findShortestPath(int startX, int startY, int endX, int endY) {
        // BFS setup
        int[][] dist = new int[width][height];
        int[][] prevX = new int[width][height];
        int[][] prevY = new int[width][height];
        boolean[][] visited = new boolean[width][height];

        for (int i = 0; i < width; i++) {
            Arrays.fill(dist[i], Integer.MAX_VALUE);
        }

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[]{startX, startY});
        visited[startX][startY] = true;
        dist[startX][startY] = 0;

        // 8 directions (including diagonals)
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int cx = current[0];
            int cy = current[1];

            if (cx == endX && cy == endY) {
                return reconstructPath(prevX, prevY, startX, startY, endX, endY, dist[endX][endY]);
            }

            for (int i = 0; i < 8; i++) {
                int nx = cx + dx[i];
                int ny = cy + dy[i];

                if (isValidPixel(nx, ny) && !visited[nx][ny] && walkable[nx][ny]) {
                    visited[nx][ny] = true;
                    dist[nx][ny] = dist[cx][cy] + 1;
                    prevX[nx][ny] = cx;
                    prevY[nx][ny] = cy;
                    queue.offer(new int[]{nx, ny});
                }
            }
        }

        // No path found
        PixelPathResult result = new PixelPathResult();
        result.path = new ArrayList<>();
        result.distance = -1;
        return result;
    }

    private boolean isValidPixel(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    private PixelPathResult reconstructPath(int[][] prevX, int[][] prevY,
                                            int startX, int startY,
                                            int endX, int endY, int distance) {
        List<int[]> path = new ArrayList<>();
        int x = endX;
        int y = endY;

        while (!(x == startX && y == startY)) {
            path.add(0, new int[]{x, y});
            int nx = prevX[x][y];
            int ny = prevY[x][y];
            x = nx;
            y = ny;
        }
        path.add(0, new int[]{startX, startY});

        PixelPathResult result = new PixelPathResult();
        result.path = path;
        result.distance = distance;
        return result;
    }

    public static class PixelPathResult {
        public List<int[]> path;
        public int distance;
    }
}