import org.archive.crawler.Heritrix;

public class Main {
    public static void main(String[] args) {
        try {
            new Heritrix().instanceMain(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
