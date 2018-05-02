package Chap05;

public class LinkList {
    private Link first;

    public LinkList() {
        this.first = null;
    }

    public void insetFirst(int id, double dd) {
        Link newLink = new Link(id, dd);
        newLink.next = first;
        first = newLink;
    }

    public Link find(int key) {
        Link current = first;
        while (current.iData != key) {
            if (current.next == null) {
                return null;
            } else
                current = current.next;
        }
        return current;
    }

    public Link delete(int key) {
        Link cuurent = first;
        Link previous = first;
        while (cuurent.iData != key) {
            if (cuurent.next == null) {
                return null;
            } else {
                previous = cuurent;
                cuurent = cuurent.next;
            }
        }

        if (cuurent == first) {
            first = first.next;
        } else previous.next = cuurent.next;

        return cuurent;
    }

    public void displayList(){
        System.out.print("List (first -- > last): ");
        Link current = first;
        while (current != null) {
            current.displayLink();
            current = current.next;
        }
        System.out.println();
    }
}
