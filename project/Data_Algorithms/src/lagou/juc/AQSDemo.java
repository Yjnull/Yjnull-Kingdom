package lagou.juc;

import java.util.concurrent.locks.ReentrantLock;

public class AQSDemo {
    private static int count = 0;
    private static ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lock();
                    for (int i = 0; i < 10000; i++) {
//                        System.out.println("Thread = " + Thread.currentThread().getName());
                        count++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        };

        Thread thread1 = new Thread(runnable);
        Thread thread2 = new Thread(runnable);
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        System.out.println("count = " + count);

        Integer a = 1;
        Integer b = 2;
        Integer c = 3;
        Integer d = 3;
        Integer e = 321;
        Integer f = 321;
        Long g = 3L;

        System.out.println(c == d); // 1
        System.out.println(e == f); // false
        System.out.println(c == (a+b)); // 0
        System.out.println(c.equals(a+b)); // 0
        System.out.println(g == (a + b)); //
        System.out.println(g.equals(a + b));
    }
}
