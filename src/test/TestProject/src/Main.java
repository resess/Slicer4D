public class  Main {
    private static int a = 5;

    public static void main(String[] args) {
        int a = 3;
        int b = 5;
        if (a > b) {
            System.out.println("ddd");
        }
        int t = test(2, 15);
        System.out.println(t);
    }
    public static int test(int x, int y) {
        int z = y - 5;
        int r = z + 5;
        int k = Integer.parseInt("4");
        if (x > 0)
            z = x + y;
        else
            z = x - y;
        if (k > 3) {
            a = 5;
        }
        if (r != 15) {
            return 1;
        } else {
            return z;
        }
    }
}



























