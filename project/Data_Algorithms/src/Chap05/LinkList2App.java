package Chap05;

public class LinkList2App {
    public static void main(String[] args) {
        LinkList theList = new LinkList();
        theList.insetFirst(22, 2.99);
        theList.insetFirst(44, 4.99);
        theList.insetFirst(66, 6.99);
        theList.insetFirst(89, 8.99);

        theList.displayList();

        Link f = theList.find(44);
        if (f != null) System.out.println("found link with key " + f.iData);
        else System.out.println("Can't find link");

        Link d = theList.delete(66);
        if (d != null) System.out.println("found link with key " + d.iData);
        else System.out.println("Can't find link");

        theList.displayList();
    }
}
