package uk.ac.nott.mrl.homework.client.ui;

public class Colour
{
   private int r, g, b;

    public Colour(int r, int g, int b)
    {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public int getRed ()
    {
        return r;
    }

    public int getGreen ()
    {
        return g;
    }

    public int getBlue ()
    {
        return b;
    }

    public String getHexValue ()
    {
    	return "#"
            + pad(Integer.toHexString(r))
            + pad(Integer.toHexString(g))
            + pad(Integer.toHexString(b));
    }
    
    public void mixWithWhite(final double percent)
    {
    	r += ((255 - r) * (1.0 - percent));
    	g += ((255 - g) * (1.0 - percent));
    	b += ((255 - b) * (1.0 - percent));    	
    }

    private String pad (String in)
    {
        if (in.length() == 0) {
            return "00";
        }
        if (in.length() == 1) {
            return "0" + in;
        }
        return in;
    }

    public String toString ()
    {
        return "red=" + r + ", green=" + g + ", blue=" + b;
    }
}