package ChapThread;

import java.io.IOException;

public class MachineIO extends Thread{
    static int count = 0;
    static StringBuffer log = new StringBuffer();

    @Override
    public void run() {
        /*for (int i = 0; i < 50; i++) {
            System.out.println(currentThread().getName() + " : " + i);
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/

        for (int i = 0; i < 20; i++) {
            log.append(currentThread().getName() + ": " + i + " ");
            if (++count % 10 == 0) log.append("\n");
            yield(); //线程让步，只会让步给相同优先级或更高优先级的线程，当前线程会进入就绪状态
        }
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        /*MachineIO m1 = new MachineIO();
        m1.start();
        int data = System.in.read();
        System.out.println("m1- is Alive : " + m1.isAlive());
        m1.run();*/

        MachineIO m1 = new MachineIO();
        m1.setName("m1");
        MachineIO m2 = new MachineIO();
        m2.setName("m2");
        Thread main = Thread.currentThread();
        System.out.println("Default priority of " + main.getName() + main.getPriority());
        System.out.println("Default priority of " + m1.getName() + m1.getPriority());
        System.out.println("Default priority of " + m2.getName() + m2.getPriority());

        /*m2.setPriority(MAX_PRIORITY);
        m1.setPriority(MIN_PRIORITY);*/

        m1.start();
        m2.start();
        while (m1.isAlive() || m2.isAlive()) {
            Thread.sleep(500);
        }
        System.out.println(log);

    }
}
