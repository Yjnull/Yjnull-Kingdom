package ChapThread;

public class MachineDaemon extends Thread {

    private int a;
    private static int count;

    @Override
    public synchronized void start() {
        super.start();
        Thread daemon = new Thread() {
            @Override
            public void run() {
                while (true) {
                    System.out.println("reset()");
                    reset();
                    try {
                        sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        daemon.setDaemon(true);
        daemon.start();
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
        MachineDaemon m1 = new MachineDaemon();
        m1.start();
    }
}
