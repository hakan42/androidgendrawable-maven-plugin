package fr.avianey.modjo.androidgendrawable;


public enum Density {
    
    LDPI(120), MDPI(160), HDPI(240), XHDPI(320), TVDPI(213), XXHDPI(480);
    
    private int dpi;

    private Density(int dpi) {
        this.dpi = dpi;
    }

    public double ratio(Density target) {
        return (double) target.dpi / (double) this.dpi;
    }
}
