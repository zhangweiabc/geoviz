package geovista.jmapviewer.tilesources;

import java.awt.Image;


import geovista.jmapviewer.Coordinate;
import geovista.jmapviewer.interfaces.TileSource;

//License: GPL. Copyright 2008 by Jan Peter Stotz

abstract public class AbstractTileSource implements TileSource {

    protected String attributionText;
    protected String attributionLinkURL;
    protected Image attributionImage;
    protected String attributionImageURL;
    protected String termsOfUseText;
    protected String termsOfUseURL;


    public boolean requiresAttribution() {
        return attributionText != null || attributionImage != null || termsOfUseText != null || termsOfUseURL != null;
    }


    public String getAttributionText(int zoom, Coordinate topLeft, Coordinate botRight) {
        return attributionText;
    }


    public String getAttributionLinkURL() {
        return attributionLinkURL;
    }


    public Image getAttributionImage() {
        return attributionImage;
    }


    public String getAttributionImageURL() {
        return attributionImageURL;
    }


    public String getTermsOfUseText() {
        return termsOfUseText;
    }


    public String getTermsOfUseURL() {
        return termsOfUseURL;
    }

    public void setAttributionText(String attributionText) {
        this.attributionText = attributionText;
    }

    public void setAttributionLinkURL(String attributionLinkURL) {
        this.attributionLinkURL = attributionLinkURL;
    }

    public void setAttributionImage(Image attributionImage) {
        this.attributionImage = attributionImage;
    }

    public void setAttributionImageURL(String attributionImageURL) {
        this.attributionImageURL = attributionImageURL;
    }

    public void setTermsOfUseText(String termsOfUseText) {
        this.termsOfUseText = termsOfUseText;
    }

    public void setTermsOfUseURL(String termsOfUseURL) {
        this.termsOfUseURL = termsOfUseURL;
    }

}
