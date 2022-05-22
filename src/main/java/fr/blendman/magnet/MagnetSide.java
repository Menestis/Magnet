package fr.blendman.magnet;

/**
 * @author Blendman974
 */
public enum MagnetSide {
    PROXY,
    SERVER;


    @Override
    public String toString() {
        if (this == PROXY)
            return "Proxy";
        else return "Server";
    }
}
