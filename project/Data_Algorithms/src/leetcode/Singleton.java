package leetcode;

public class Singleton {
    /*private static class LazyHolder{
        private static final Singleton instance = new Singleton();
    }

    private Singleton(){}

    public static Singleton getInstance(){
        return LazyHolder.instance;
    }*/


    private static Singleton instance = new Singleton();

    public static Singleton getInstance(){
        return instance;
    }
}
