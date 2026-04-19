package ru.tbank.tmap.exception.heatmap;

public class ClusterNotFoundException extends RuntimeException {

    private final String h3Index;

    public ClusterNotFoundException(final String h3Index) {
        super("Cluster not found");
        this.h3Index = h3Index;
    }

    public String getH3Index() {
        return h3Index;
    }
}
