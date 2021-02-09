class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("MyThread running");
    }
}  

public class Main {
    public static void main(String[] args) {
        MyThread myThread = new MyThread();
        myThread.start();
    }
}