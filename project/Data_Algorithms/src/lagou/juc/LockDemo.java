package lagou.juc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockDemo {

    public static void main(String[] args) throws InterruptedException {
        Lock lock = new ReentrantLock();
        Condition conditionA = lock.newCondition();
        Condition conditionB = lock.newCondition();
        Condition conditionC = lock.newCondition();
        Condition conditionMain = lock.newCondition();

        Integer.toBinaryString(3);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lock();
                    for (int i = 1; i <= 10; i++) {
                        System.out.println("A" + i);
                        conditionB.signal();
                        conditionA.await();
                    }
                    System.out.println("A 结束");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    conditionB.signal();
                    lock.unlock();
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lock();
                    for (int i = 1; i <= 10; i++) {
                        System.out.println("B"+i);
                        conditionC.signal();
                        conditionB.await();
                    }
                    System.out.println("B 结束");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    conditionC.signal();
                    lock.unlock();
                }
            }
        }).start();

        Thread c = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lock();
                    for (int i = 1; i <= 10; i++) {
                        System.out.println("C" + i);
                        conditionA.signal();
                        conditionC.await();
                    }
                    System.out.println("C 结束");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        });
        c.start();
        c.join();


//        lock.lock();
//        conditionMain.await();
//        conditionMain.signalAll();
        System.out.println("全部结束...");
//        lock.unlock();
    }
}
