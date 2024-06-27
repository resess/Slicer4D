
import java.lang.Thread.UncaughtExceptionHandler;

public class DynamicSlicingLoggerExceptionHandler implements Thread.UncaughtExceptionHandler {


    @Override
    public void uncaughtException(Thread th, Throwable ex) {
        ex.printStackTrace();
        System.out.println("Slicer caught exception: " + ex.toString());
        DynamicSlicingLogger.flush();
    }
}