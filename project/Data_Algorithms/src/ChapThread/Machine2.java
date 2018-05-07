package ChapThread;

import java.util.ArrayList;
import java.util.Collections;

public class Machine2 extends Thread {

    private int a = 1;

    public synchronized void print() {
        System.out.println(Thread.currentThread().getName() + " a = " + a);
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            a = 1/0;
            a++;
            print();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Machine2 m2 = new Machine2();
        m2.start();
        Thread.yield();
        m2.print();
    }
}
