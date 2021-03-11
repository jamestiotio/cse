class MyRunnable implements Runnable {
    public void run() {
        System.out.println("MyRunnable running");
    }
}

public class Main {
    public static void main(String[] args) {
        Runnable runnable = new MyRunnable(); 
        Thread thread = new Thread(runnable);
        thread.start();
    }
}