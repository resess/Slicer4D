public class DynamicSlicingLoggerShutdown extends Thread {
    DynamicSlicingLoggerShutdown() {
    }

    @Override
    public void run() {
        System.out.println("Shutting down VM");
        DynamicSlicingLogger.flush();
    }
}