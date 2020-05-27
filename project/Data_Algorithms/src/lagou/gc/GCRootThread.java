package lagou.gc;

public class GCRootThread {

    private int _10MB = 10 * 1024 * 1024;
    private byte[] memory = new byte[8 * _10MB];

    public static void main(String[] args) throws InterruptedException {
        System.out.println("开始内存情况：");
        printMemory();

        method();

        System.gc();
        System.out.println("main 执行完，完成 GC");
        printMemory();

        // thread.join();

        /*asyncTask = null;
        thread = null;*/
        Thread.sleep(500);
        System.gc();
        System.out.println("线程代码执行完，完成 GC");
        printMemory();
    }

    private static void method() {
        AsyncTask asyncTask = new AsyncTask(new GCRootThread());
        Thread thread = new Thread(asyncTask);
        thread.start();
    }


    private static void printMemory() {
        System.out.print("free is " + Runtime.getRuntime().freeMemory()/1024/1024 + " M, ");
        System.out.println("total is " + Runtime.getRuntime().totalMemory()/1024/1024 + " M, ");
    }

    private static class AsyncTask implements Runnable {

        private GCRootThread gcRootThread;

        public AsyncTask(GCRootThread gcRootThread) {
            this.gcRootThread = gcRootThread;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

            }
        }
    }
}
