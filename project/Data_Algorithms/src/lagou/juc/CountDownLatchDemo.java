package lagou.juc;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchDemo {
    private static String a = "1";
    private static String b = "1";

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Thread1 run...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                countDownLatch.countDown();
                a = "1结束";
                System.out.println("Thread1 run 结束" + countDownLatch.getCount());
            }
        });

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Thread2 run...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                b = "2结束";
                System.out.println("Thread2 run 结束" + countDownLatch.getCount());
            }
        });

        thread1.start();
        thread2.start();

        System.out.println("等待两个线程执行完 " + countDownLatch.getCount());
        countDownLatch.await();
        System.out.println("继续主线程的任务 " + a + b);

    }
}
