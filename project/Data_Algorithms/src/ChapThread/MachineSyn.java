package ChapThread;

public class MachineSyn implements Runnable {
    private int a = 1;

    @Override
    public void run() {
        for (int i = 0; i < 1000; i++) {
            /*synchronized (this) {
                a+=i;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                a-=i;
                System.out.println(Thread.currentThread().getName() + ": " + a);
            }*/

            a+=i;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            a-=i;
            System.out.println(Thread.currentThread().getName() + ": " + a);
        }
    }

    public void go() {
        for (int i = 0; i < 1000; i++) {
            System.out.println(Thread.currentThread().getName() + "i: " + i);
            Thread.yield();
        }
    }

    public static void main(String[] args) {
        MachineSyn m1 = new MachineSyn();
        Thread t1 = new Thread(m1);
        Thread t2 = new Thread(m1);
        t1.start();
        t2.start();
        m1.go();
    }
}
