package vRouter;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ExcelLogger {

    public static class SimpleLogger {
        private static final String FILE_PATH = "D:\\access_log.csv";
        private static final int FLUSH_THRESHOLD = 1000;

        private static final List<String> buffer = new ArrayList<String>();
        private static boolean headerWritten = false;

        public static synchronized void logSimpleAccess(long cycle, String dataId, int accessCount,
                                                        int nodeCount, double score, int recycled, int level) {
            String row = String.format("%d,%s,%d,%d,%.4f,%d,%d",
                    cycle, dataId, accessCount, nodeCount, score, recycled,level);
            buffer.add(row);

            if (buffer.size() >= FLUSH_THRESHOLD||cycle>=128) {
                flushBuffer();
            }
        }

        public static synchronized void flushBuffer() {
            if (buffer.isEmpty()) {
                return;
            }

            BufferedWriter bw = null;
            try {
                File file = new File(FILE_PATH);
                boolean isNewFile = !file.exists();

                FileWriter fw = new FileWriter(file, true);
                bw = new BufferedWriter(fw);

                if (isNewFile || !headerWritten) {
                    bw.write("cycle,data_id,access_count,node_count,score,recycled,level");
                    bw.newLine();
                    headerWritten = true;
                }

                for (String row : buffer) {
                    bw.write(row);
                    bw.newLine();
                }

                buffer.clear();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bw != null) {
                    try {
                        bw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public static void closeLogger() {
            flushBuffer();
        }
    }
}