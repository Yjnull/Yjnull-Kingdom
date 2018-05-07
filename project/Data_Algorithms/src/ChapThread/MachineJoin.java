package ChapThread;

public class MachineJoin extends Thread {
    @Override
    public void run() {
        for (int i = 0; i < 50; i++) {
            System.out.println(currentThread().getName() + " : " + i);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        MachineJoin machineJoin = new MachineJoin();
        machineJoin.setName("m1");

        machineJoin.start();

        System.out.println("main : join machine");
        machineJoin.join();  //等待其他线程结束：main 线程进入阻塞状态，等待m1线程运行结束
        System.out.println("main : join machine end");
    }
}
