package gallery.routefinder.db;

import gallery.routefinder.graph.Graph;
import gallery.routefinder.graph.GraphNode;
import gallery.routefinder.model.Room;
import gallery.routefinder.model.Artwork;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DataLoader {

    public static Graph loadFromCSV(String roomsPath, String connectionsPath, String artworksPath) throws IOException {
        Graph graph = new Graph();

        // get project path
        String projectPath = System.getProperty("user.dir");
        String basePath = projectPath + "/src/main/resources/";

        System.out.println("Loading from: " + basePath);

        // loading rooms
        File roomsFile = new File(basePath + roomsPath);
        if (!roomsFile.exists()) {
            throw new IOException("File not found: " + roomsFile.getAbsolutePath());
        }
        List<String[]> rooms = readCSVFile(roomsFile);
        for (String[] row : rooms) {
            if (row[0].equals("id")) continue;
            Room room = new Room(row[0], row[1], Integer.parseInt(row[2]), Integer.parseInt(row[3]));
            graph.addNode(new GraphNode(room));
        }
        System.out.println("Rooms loaded: " + (rooms.size() - 1));

        // load connections (hallways between rooms)
        File connectionsFile = new File(basePath + connectionsPath);
        if (!connectionsFile.exists()) {
            throw new IOException("File not found: " + connectionsFile.getAbsolutePath());
        }
        List<String[]> connections = readCSVFile(connectionsFile);
        for (String[] row : connections) {
            if (row[0].equals("from")) continue;
            graph.addUndirectedEdge(row[0], row[1], Double.parseDouble(row[2]));
        }
        System.out.println("Connections loaded: " + (connections.size() - 1));

        // loading artworks (paintings in each room)
        File artworksFile = new File(basePath + artworksPath);
        if (!artworksFile.exists()) {
            throw new IOException("File not found: " + artworksFile.getAbsolutePath());
        }
        List<String[]> artworks = readCSVFile(artworksFile);
        for (String[] row : artworks) {
            if (row[0].equals("roomId")) continue;
            Artwork artwork = new Artwork(row[1], row[2], row[3]);
            GraphNode node = graph.getNodeById(row[0]);
            if (node != null) {
                node.getRoom().addArtwork(artwork);
            }
        }
        System.out.println("Artworks loaded: " + (artworks.size() - 1));

        return graph;
    }

    private static List<String[]> readCSVFile(File file) throws IOException {
        List<String[]> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                result.add(line.split(","));
            }
        }
        return result;
    }
}