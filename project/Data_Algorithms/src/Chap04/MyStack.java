package Chap04;

public class MyStack<T> {
    private int maxSize;
    private T[] stackArray;
    private int top;

    public MyStack(int s) {
        maxSize = s;
        stackArray = (T[]) new Object[maxSize];
        top = -1;
    }

    public void push(T j) {
        if (!isFull()) {
            stackArray[++top] = j;
        }
    }

    public T pop(){
        if (!isEmpty()) {
            return stackArray[top--];
        }
        return null;
    }

    public T peek(){
        if (!isEmpty()) {
            return stackArray[top];
        }
        return null;
    }

    public boolean isEmpty(){
        return top == -1;
    }

    public boolean isFull(){
        return top == maxSize - 1;
    }

    public int size(){
        return top + 1;
    }

    public T peekN(int n) {
        if (!isEmpty()) return stackArray[n];
        return null;
    }

    public void displayStack(String s) {
        System.out.print(s);
        System.out.print("Stack (bottom-->top): ");
        for (int i = 0; i < size(); i++) {
            System.out.print( peekN(i));
            System.out.print( ' ');
        }
        System.out.println();
    }
}
