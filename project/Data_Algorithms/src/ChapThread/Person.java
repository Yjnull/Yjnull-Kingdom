package ChapThread;

public class Person extends Thread {
    private Well well;

    public Person(Well well) {
        this.well = well;
        start();
    }

    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            well.withdraw();
            yield();
        }
    }

    public static void main(String[] args) {
        Well well = new Well();
        Person[] person = new Person[10];
        for (int i = 0; i < 10; i++) {
            person[i] = new Person(well);
        }
    }


}

class Well{
    private int water = 1000;
    public synchronized void withdraw() {
        water-=10;
        System.out.println(Thread.currentThread().getName() + ": water left: " + water);
    }
}
