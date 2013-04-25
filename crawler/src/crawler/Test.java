package crawler;

public class Test {
    public static void main(String[] args) throws Exception {
    	Master master = new Master(10, "jdbc:sqlite:///tmp/tmp.db", "http://en.wikipedia.org/wiki/Main_page/", 3);
    	Thread t = new Thread(master);
        t.setName("master");
        t.start();
        t.join();
    }
}