public class Main2 {
    public static int test(int x, int y) {
        int z = y - 5;
        int r = z + 5;
        int k = Integer.parseInt("4");
        if (x > 0)
            z = x + y;
        else
            z = x - y;
        if (k > 3) {
//            a = 5;
        }
        if (r != 15) {
            return 1;
        } else {
            return z;
        }
    }
}
