package Chap08;

public class Tree {
    private Node root;  // the only data field in Tree

    public Node find(int key) {
        Node current = root;

        while (current.iData != key) {
            if (key < current.iData)
                current = current.leftChild;
            else
                current = current.rightChild;

            if (current == null)
                return null;
        }

        return current;
    }

    public void insert(int id, double dd) {

    }

    public void delete(int id) {

    }

}
