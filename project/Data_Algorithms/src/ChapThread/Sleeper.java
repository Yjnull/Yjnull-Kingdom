package ChapThread;

public class Sleeper extends Thread {
    @Override
    public void run() {
        try {
            sleep(6000);
            System.out.println("sleep over");
        } catch (InterruptedException e) {
            System.out.println("sleep interrupted");
        }
        System.out.println("end");
    }

    public static void main(String[] args) throws InterruptedException {
        Sleeper sleeper = new Sleeper();
        sleeper.start();
        Thread.sleep(10);
        sleeper.interrupt();
    }
}
