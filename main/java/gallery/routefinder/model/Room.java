package gallery.routefinder.model;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private String id;
    private String name;
    private int x, y;
    private List<Artwork> artworks;

    public Room(String id, String name, int x, int y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.artworks = new ArrayList<>();
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getX() { return x; }
    public int getY() { return y; }
    public List<Artwork> getArtworks() { return artworks; }

    public void addArtwork(Artwork artwork) {
        artworks.add(artwork);
    }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}