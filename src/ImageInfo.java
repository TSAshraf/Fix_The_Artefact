// Class to hold extra information about an image
public class ImageInfo {
    private String description; // Description of the image
    private String url; // URL providing sources of image and extra information

    // Constructor to initialise the ImageInfo with a description and URL
    public ImageInfo(String description, String url) {
        this.description = description;
        this.url = url;
    }

    // Returns the description of the image
    public String getDescription() {
        return description;
    }

    // Returns the URL of the image
    public String getUrl() {
        return url;
    }
}