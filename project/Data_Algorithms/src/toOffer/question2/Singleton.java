package toOffer.question2;

public class Singleton {
    private Singleton(){}

    /**
     *  解法一： 双重校验锁
     *  instance声明为volatile的原因
     *  [A] 操作并不是原子操作，这句代码在JVM中大概做了三件事
     *      1. 给instance分配内存
     *      2. 调用Singleton的构造函数来初始化成员变量
     *      3. 将instance对象指向分配的内存空间（执行完这步instance就非 null 了）
     *      但是在 JVM 的即时编译器中存在指令重排序的优化,也就是说执行顺序可能是1-2-3，也可能是1-3-2
     *  使用 volatile 的主要原因：禁止指令重排序优化
     *  注意：Java 5 以前的 JMM （Java 内存模型）是存在缺陷的，即使将变量声明成 volatile 也不能完全避免重排序，
     *  主要是 volatile 变量前后的代码仍然存在重排序问题。这个 volatile 屏蔽重排序的问题在 Java 5 中才得以修复，所以在这之后才可以放心使用 volatile。
     *
     *  缺点：1. Java 5 以前还是不能完全避免重排序
     *       2. 实现复杂
     */
    /*private volatile static Singleton instance;
    public static Singleton getSingleInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null)
                    instance = new Singleton();   // [A]
            }
        }
        return instance;
    }*/


    /**
     *  解法二：饿汉式
     *  因为单例的实例被声明成 static 和 final 变量了，在第一次加载类到内存中时就会初始化，所以创建实例本身是线程安全的。
     *
     *  缺点：它不是一种懒加载模式（lazy initialization），单例会在加载类后一开始就被初始化，即使客户端没有调用 getInstance()方法。
     *  饿汉式的创建方式在一些场景中将无法使用：譬如 Singleton 实例的创建是依赖参数或者配置文件的，
     *  在 getInstance() 之前必须调用某个方法设置参数给它，那样这种单例写法就无法使用了。
     *
     *  例：
     *  用户得先在 Application 中调用init初始化一些设置，才能创建实例。
     *  public static Singleton init(@NonNull Context context) {
     *         initLogger(context);
     *         Logger.i("初始化 Singleton");
     *
     *         initImplement(context);
     *         //一些操作
     *
     *         return getSingleInstance();
     *     }
     */
    /*private static final Singleton instance = new Singleton();
    public static Singleton getSingleInstance() {
        return instance;
    }*/


    /**
     *  解法三：静态内部类 (Effective Java 推荐)
     *  利用了JVM本身机制保证了线程安全，解法二饿汉式只要 Singleton 类被装载了，那么 instance 就会被实例化（没有达到 lazy loading 效果），
     *  而这种方式是 Singleton 类被装载了，instance 不一定被初始化。因为 SingletonHolder 类没有被主动使用，
     *  只有通过显式调用 getInstance 方法时，才会显式装载 SingletonHolder 类，从而实例化 instance。
     */
    private static class SingletonHolder{
        private static final Singleton INSTANCE = new Singleton();
    }
    public static final Singleton getSingleInstance() {
        return SingletonHolder.INSTANCE;
    }

}
