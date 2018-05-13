package Chap08;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TreeApp {

    public static void main(String[] args) throws IOException{
        int value;
        Tree theTree = new Tree();

        theTree.insert(50, 1.5);
        theTree.insert(25, 1.7);
        theTree.insert(75, 1.9);
        theTree.insert(12, 1.9);
        theTree.insert(37, 1.9);
        theTree.insert(43, 1.9);
        theTree.insert(30, 1.9);
        theTree.insert(33, 1.9);
        theTree.insert(87, 1.9);
        theTree.insert(93, 1.9);
        theTree.insert(97, 1.9);

        while (true) {
            System.out.print("Enter first letter of show, ");
            System.out.print("insert, find, delete, or traverse: ");
            int choice = getChar();
            switch (choice) {
                case 's':
                    theTree.displayTree();
                    break;
                case 'i':
                    System.out.print("Enter value to insert: ");
                    value = getInt();
                    theTree.insert(value, value + 0.9);
                    break;
                case 'f':
                    System.out.print("Enter value to find: ");
                    value = getInt();
                    Node found = theTree.find(value);
                    if (found != null) {
                        System.out.print("Found: ");
                        found.displayNode();
                        System.out.println();
                    } else {
                        System.out.print("Could not found " + value + '\n');
                    }
                    break;
                case 'd':
                    System.out.print("Enter value to delete: ");
                    value = getInt();
                    boolean didDel = theTree.delete(value);
                    if (didDel) {
                        System.out.print("Deleted " + value + '\n');
                    } else {
                        System.out.print("Could not delete " + value + '\n');
                    }
                    break;
                case 't':
                    System.out.print("Enter type 1, 2 or 3: ");
                    value = getInt();
                    theTree.traverse(value);
                    System.out.println();
                    break;
                 default:
                    break;
            }
        }
    }

    public static String getString() throws IOException
    {
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(isr);
        return br.readLine();
    }

    public static char getChar() throws IOException
    {
        String s = getString();
        return s.charAt(0);
    }

    public static int getInt() throws IOException
    {
        return Integer.parseInt(getString());
    }
}
