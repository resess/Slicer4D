import java.io.PrintStream;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterOutputStream;
import java.util.Base64;


public class DynamicSlicingLogger{
    static final int SIZE = 1024;
    static final String [] queue = new String[SIZE];
    static final long [] threadIds = new long[SIZE];
    static final int [] hashCodes = new int[SIZE];
    static int queueIndex = 0;
    static final PrintStream outStream = System.out;
    static {
        Runtime.getRuntime().addShutdownHook(new DynamicSlicingLoggerShutdown());
        Thread.setDefaultUncaughtExceptionHandler(new DynamicSlicingLoggerExceptionHandler());
    }

    // public static void println(String e) {
    //     synchronized (queue) {
    //         System.out.println("SLICING: "+e);
    //     }
    // }

    public static void println(String e) {
        synchronized (queue) {
            queue[queueIndex] = e;
            threadIds[queueIndex] = Thread.currentThread().getId();
            hashCodes[queueIndex] = 0;
            queueIndex++;
            if (queueIndex > SIZE-1) {
                flush();
            }
        }
    }

    public static void println(String e, int code) {
        synchronized (queue) {
            queue[queueIndex] = e;
            threadIds[queueIndex] = Thread.currentThread().getId();
            hashCodes[queueIndex] = code;
            queueIndex++;
            if (queueIndex > SIZE-1) {
                flush();
            }
        }
    }

    public static void flush(String e) {
        synchronized (queue) {
            queue[queueIndex] = e;
            threadIds[queueIndex] = Thread.currentThread().getId();
            hashCodes[queueIndex] = 0;
            queueIndex++;
            flush();
        }
    }

    public static void flush() {
        synchronized (queue) {
            outStream.println("Flushing queue: size is " + queueIndex);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < queueIndex; i++) {
                sb.append(queue[i]);
                sb.append(":");
                sb.append(threadIds[i]);
                int code = hashCodes[i];
                if (code != 0) {
                    sb.append(":");
                    sb.append(hashCodes[i]);
                }
                sb.append("-");
            }
            byte[] bArray = sb.toString().getBytes();
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                DeflaterOutputStream dos = new DeflaterOutputStream(os);
                dos.write(bArray);
                dos.close();
                String compressed = new String(Base64.getEncoder().encode(os.toByteArray()));
                os.close();
                outStream.println("SLICING: ZLIB: " + compressed);
            } catch (Exception e){}
            queueIndex = 0;
        }
    }
}