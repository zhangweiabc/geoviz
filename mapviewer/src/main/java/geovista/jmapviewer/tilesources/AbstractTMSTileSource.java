package geovista.jmapviewer.tilesources;

//License: GPL.

import java.io.IOException;

public abstract class AbstractTMSTileSource extends AbstractTileSource {

    protected String name;
    protected String baseUrl;

    public AbstractTMSTileSource(String name, String base_url) {
        this.name = name;
        this.baseUrl = base_url;
        if(baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0,baseUrl.length()-1);
        }
    }


    public String getName() {
        return name;
    }


    public int getMaxZoom() {
        return 21;
    }


    public int getMinZoom() {
        return 0;
    }

    public String getExtension() {
        return "png";
    }

    /**
     * @throws IOException when subclass cannot return the tile URL
     */
    public String getTilePath(int zoom, int tilex, int tiley) throws IOException {
        return "/" + zoom + "/" + tilex + "/" + tiley + "." + getExtension();
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }


    public String getTileUrl(int zoom, int tilex, int tiley) throws IOException {
        return this.getBaseUrl() + getTilePath(zoom, tilex, tiley);
    }

    @Override
    public String toString() {
        return getName();
    }


    public String getTileType() {
        return "png";
    }


    public int getTileSize() {
        return 256;
    }


    public double latToTileY(double lat, int zoom) {
        double l = lat / 180 * Math.PI;
        double pf = Math.log(Math.tan(l) + (1 / Math.cos(l)));
        return Math.pow(2.0, zoom - 1) * (Math.PI - pf) / Math.PI;
    }


    public double lonToTileX(double lon, int zoom) {
        return Math.pow(2.0, zoom - 3) * (lon + 180.0) / 45.0;
    }


    public double tileYToLat(int y, int zoom) {
        return Math.atan(Math.sinh(Math.PI - (Math.PI * y / Math.pow(2.0, zoom - 1)))) * 180 / Math.PI;
    }


    public double tileXToLon(int x, int zoom) {
        return x * 45.0 / Math.pow(2.0, zoom - 3) - 180.0;
    }
}
