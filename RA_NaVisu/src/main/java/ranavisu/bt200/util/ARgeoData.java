package ranavisu.bt200.util;

/**
 *
 * @author JP M
 */
public class ARgeoData {
     
    private double lat;

    private double lon;
    
    private String imageAddress;
    
    private String name;

    private String type;

    /**
     * Get the value of name
     *
     * @return the value of name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the value of name
     *
     * @param name new value of name
     */
    public void setName(String name) {
        this.name = name;
    }

    public ARgeoData(double lat, double lon, String imageAddress) {
        this.lat = lat;
        this.lon = lon;
        this.imageAddress = imageAddress;
    }

    public ARgeoData(double lat, double lon, String imageAddress, String name) {
        this.lat = lat;
        this.lon = lon;
        this.imageAddress = imageAddress;
        this.name = name;
    }

    public ARgeoData(double lat, double lon, String imageAddress, String name, String type) {
        this.lat = lat;
        this.lon = lon;
        this.imageAddress = imageAddress;
        this.name = name;
        this.type = type;
    }

    public ARgeoData() {
    }    
    

    /**
     * Get the value of imageAddress
     *
     * @return the value of imageAddress
     */
    public String getImageAddress() {
        return imageAddress;
    }

    /**
     * Set the value of imageAddress
     *
     * @param imageAddress new value of imageAddress
     */
    public void setImageAddress(String imageAddress) {
        this.imageAddress = imageAddress;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get the value of lon
     *
     * @return the value of lon
     */
    public double getLon() {
        return lon;
    }

    /**
     * Set the value of lon
     *
     * @param lon new value of lon
     */
    public void setLon(double lon) {
        this.lon = lon;
    }

    /**
     * Get the value of lat
     *
     * @return the value of lat
     */
    public double getLat() {
        return lat;
    }

    /**
     * Set the value of lat
     *
     * @param lat new value of lat
     */
    public void setLat(double lat) {
        this.lat = lat;
    }

    @Override
    public String toString() {
        return "ARgeoData{" +
                "lat=" + lat +
                ", lon=" + lon +
                ", imageAddress='" + imageAddress + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
