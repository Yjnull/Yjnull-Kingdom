package ChapThread;

import java.util.Timer;
import java.util.TimerTask;

public class MachineTimer extends Thread {

    private int a;
    private static int count;

    @Override
    public synchronized void start() {
        super.start();

        Timer timer = new Timer(true);

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                while (true) {
                    System.out.println("reset: " + currentThread().getName());
                    reset();
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        timer.schedule(task, 1, 500);
    }

    private void reset() {
        a = 0;
    }

    @Override
    public void run() {
        while (true) {
            System.out.println(getName() + ": " + a++);
            if (count++ == 100) break;
            yield();
        }
    }

    public static void main(String[] args) {
        MachineTimer m1 = new MachineTimer();
        m1.start();
    }
}
