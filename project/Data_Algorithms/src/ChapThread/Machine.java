package ChapThread;

public class Machine extends Thread{
    static int count = 0;
    int i = 0;

    @Override
    public synchronized void start() {
        super.start();
        System.out.println(currentThread().getName()
                + "：第" + ++count + "个 Machine 线程启动");
    }

    @Override
    public void run() {
        for (i = 0; i < 50; i++) {
            System.out.println(currentThread().getName() + " : " + i);
            /*try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
        }
    }


    public static void main(String[] args) {
        Machine m1 = new Machine();
        m1.setName("Machine->m1");
        Machine m2 = new Machine();
        m2.setName("Machine->m2");

        /*Thread main = Thread.currentThread();

        System.out.println("Default priority of " + main.getName() + main.getPriority());
        System.out.println("Default priority of " + m1.getName() + m1.getPriority());
        System.out.println("Default priority of " + m2.getName() + m2.getPriority());*/


        m1.start();
        m2.start();
        //m2.start();  一个线程只能被启动一次，启动两次会抛出 java.lang.IllegalThreadStateException
    }
}
