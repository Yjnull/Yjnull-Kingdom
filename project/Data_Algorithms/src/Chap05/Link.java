package Chap05;

public class Link {
    public int iData;
    public double dData;
    public Link next;

    public Link(int iData, double dData) {
        this.iData = iData;
        this.dData = dData;
    }

    public void displayLink(){
        System.out.print("{" + iData + ", " + dData + "}");
    }
}
