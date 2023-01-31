import java.io.*;
import java.util.HashSet;

public class Main {
    //How it works:
    // Ip is 4 bytes and have next format in bits 8.8.8.8
    // This is equal to integer = 32bit
    // Everything we need is one bit per ip (=per integer) (did we face this int before)
    // So we can use simple counting
    // To spend less ram space we can store bits into int variables and int's into array
    // Example if (assume a block equal to 10 not to 32):
    // block: 0         1         2
    // bit:   012345678901234567890123456789
    // value: 000000000000000000000000000000
    // we faced ip = 0.0.0.14
    // convert it into int => 0*24 + 0*16 + 0*8 + 14 = 14
    // block: 0         1         2
    // bit:   012345678901234567890123456789
    // value: 000000000000001000000000000000
    // etc.
    // also we use long to avoid negative numbers (there is no usigned int in java)
    static class Bitmask {
        // possible bits 8 bit * 4 = 32 bit => there are 2^32 possible combinations => we need 2^32 bits to store it
        // int can store 32 bit = 2^4 => we need 2^32/2^4 = 2^28 ints = 268435456
        private final int[] bitmask = new int[268435456];
        private long counter = 0;

        private long defineBlock(long pos){
            return pos / 32;
        }

        private long definePosInBlock(long pos){
            return pos % 32;
        }

        private boolean getBit(int block, long pos){
            return ((bitmask[block] >> pos) & 1) != 0;
        }

        public void setBit(long pos){
            int block = (int)defineBlock(pos);
            long posInBlock = definePosInBlock(pos);

            if (!getBit(block,posInBlock)){
                bitmask[block] |= 1L << posInBlock;
                counter++;
            }
        }

        private long getCounter(){
            return counter;
        }

    }

    static Bitmask bitmask = new Bitmask();

    public static long parseIp(String ip){
        String[] bytes = ip.split("\\.");
        long res = 0;
        res += Integer.parseInt(bytes[0]);
        res = res << 8;
        res += (Integer.parseInt(bytes[1]));
        res = res << 8;
        res += (Integer.parseInt(bytes[2]));
        res = res << 8;
        res += (Integer.parseInt(bytes[3]));
        return res;
    }
    private static void handleIp(String string){
        long ipAsInt = parseIp(string);
        bitmask.setBit(ipAsInt);
    }

    public static void main(String[] args) {
        String filename = "bigfile.txt";
        File file = new File(filename);
        HashSet<Integer> map = new HashSet<>();

        try (InputStream in = new FileInputStream(file)) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
            String line = bufferedReader.readLine();

            while (line != null){
                handleIp(line);
                line = bufferedReader.readLine();
            }
        }
        catch (Exception ex) {
            System.out.println(ex);
        }

        System.out.println("Result: " + bitmask.getCounter());
    }
}