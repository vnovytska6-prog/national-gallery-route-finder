package gallery.routefinder;

import gallery.routefinder.db.DataLoader;
import gallery.routefinder.graph.Graph;
import gallery.routefinder.graph.GraphNode;

public class Main {
    public static void main(String[] args) {
        System.out.println("Gallery Route Finder\n");

        try {
            // loading data from csv
            Graph graph = DataLoader.loadFromCSV(
                    "rooms.csv",
                    "connections.csv",
                    "artworks.csv"
            );

            System.out.println("Loaded rooms: " + graph.getAllNodes().size());

            //  showing all rooms and artworks in there
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
}