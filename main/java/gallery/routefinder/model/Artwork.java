package gallery.routefinder.model;

public class Artwork {
    private String title;
    private String artist;
    private String thumbnailPath;

    public Artwork(String title, String artist, String thumbnailPath) {
        this.title = title;
        this.artist = artist;
        this.thumbnailPath = thumbnailPath;
    }

    // Getters
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getThumbnailPath() { return thumbnailPath; }

    @Override
    public String toString() {
        return title + " by " + artist;
    }
}