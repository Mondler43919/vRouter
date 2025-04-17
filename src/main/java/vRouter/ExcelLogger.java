package vRouter;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.math.BigInteger;

public class ExcelLogger {
    private static final String FILE_PATH = "D:\\data_access_log.xlsx";

    // 记录数据访问日志
    public static void logDataAccess(long cycle, long timestamp,BigInteger msg_to, BigInteger msg_from, BigInteger dataId,String hash) {
        File file = new File(FILE_PATH);
        Workbook workbook;
        Sheet sheet;

        try {
            // 如果文件已存在，加载它，否则创建新文件
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                workbook = new XSSFWorkbook(fis); // 打开已存在的文件
                fis.close();
                sheet = workbook.getSheet("AccessLog");
                if (sheet == null) {
                    sheet = workbook.createSheet("AccessLog");
                }
            } else {
                workbook = new XSSFWorkbook(); // 创建新工作簿
                sheet = workbook.createSheet("AccessLog");

                // 创建标题行
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("cycle");
                header.createCell(1).setCellValue("timestamp");
                header.createCell(2).setCellValue("local_node");
                header.createCell(3).setCellValue("msg_from");
                header.createCell(4).setCellValue("data_id");
                header.createCell(5).setCellValue("hash");
            }
            // 找到最后一行索引并添加数据
            int lastRowNum = sheet.getLastRowNum();
            Row row = sheet.createRow(lastRowNum + 1);  // 在最后一行后面添加新行

            // 写入数据
            row.createCell(0).setCellValue(cycle);
            row.createCell(1).setCellValue(timestamp);
            row.createCell(2).setCellValue(msg_to.toString());
            row.createCell(3).setCellValue(msg_from.toString());
            row.createCell(4).setCellValue(dataId.toString());
            row.createCell(5).setCellValue(hash);

            // 写回文件
            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            fos.close();
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
