package be.webtechie.crac.example.manager;

import be.webtechie.crac.example.database.AppLog;
import be.webtechie.crac.example.database.Dao;
import be.webtechie.crac.example.model.DataRecord;
import be.webtechie.crac.example.model.DataSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CsvManager {

    public static final String[] FILES = new String[]{
            "organizations-1000.csv",
            "organizations-10000.csv",
            "organizations-100000.csv",
            "organizations-500000.csv",
            "organizations-1000000.csv"
    };
    private static final Logger LOGGER = LogManager.getLogger(CsvManager.class);
    private static Dao<AppLog, Integer> appLogDao;
    private final List<DataSet> dataSets;

    public CsvManager(Dao<AppLog, Integer> appLogDao) {
        CsvManager.appLogDao = appLogDao;
        dataSets = new ArrayList<>();
    }

    public DataSet getDataSet(String fileName) {
        var dataSet = dataSets.stream()
                .filter(ds -> ds.fileName().equals(fileName))
                .findFirst();
        if (dataSet.isPresent()) {
            LOGGER.info("Data from {} is already loaded", fileName);
            return dataSet.get();
        }
        DataSet newDataSet = loadData(fileName);
        dataSets.add(newDataSet);
        return newDataSet;
    }

    private DataSet loadData(String fileName) {
        LOGGER.warn("Need to load data from {}", fileName);
        var start = System.currentTimeMillis();
        List<DataRecord> records = new ArrayList<>();
        var fileContent = getContentFromZip(fileName);
        try (Scanner scanner = new Scanner(fileContent)) {
            while (scanner.hasNextLine()) {
                records.add(convertOrganizationData(scanner.nextLine()));
            }
        } catch (Exception e) {
            LOGGER.error("Could not handle data from file {}", fileName);
        }
        var end = System.currentTimeMillis();
        appLogDao.save(new AppLog("Data was loaded and converted to Java objects from " + fileName, (int) (end - start)));
        LOGGER.warn("Data from {} was loaded and converted to Java objects in {}ms", fileName, (end - start));
        return new DataSet(fileName, records);
    }

    /**
     * Based on https://www.baeldung.com/java-compress-and-uncompress
     */
    public String getContentFromZip(String fileName) {
        var filePath = "/data/" + fileName + ".zip";
        String rt = "";
        LOGGER.warn("Unzipping data file {}", filePath);
        var start = System.currentTimeMillis();
        ByteArrayOutputStream fos = null;
        try (InputStream is = getClass().getResourceAsStream(filePath)) {
            if (is == null) {
                LOGGER.error("No data could be read from {}", filePath);
            } else {
                byte[] buffer = new byte[1024];
                ZipInputStream zis = new ZipInputStream(is);
                ZipEntry zipEntry = zis.getNextEntry();
                fos = new ByteArrayOutputStream();
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                zis.closeEntry();
                zis.close();
                rt = fos.toString(StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            LOGGER.error("Could not find the file {} to be loaded", filePath);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    LOGGER.error("Error while closing file {}: {}", filePath, e.getMessage());
                }
            }
        }
        var end = System.currentTimeMillis();
        appLogDao.save(new AppLog("ZIP was unpacked from " + fileName, (int) (end - start)));
        LOGGER.warn("Data from ZIP with length {} was loaded in {}ms", rt.length(), (end - start));
        return rt;
    }

    private DataRecord convertOrganizationData(String data) {
        String[] elements = data.split(",");
        return new DataRecord(
                Long.parseLong(getDataElement(elements, 0)),
                getDataElement(elements, 1),
                getDataElement(elements, 2),
                getDataElement(elements, 3),
                getDataElement(elements, 4),
                getDataElement(elements, 5),
                getDataElement(elements, 6),
                getDataElement(elements, 7),
                Long.parseLong(getDataElement(elements, 8))
        );
    }

    private String getDataElement(String[] elements, int index) {
        if (elements.length < index - 1) {
            return "";
        }
        return elements[index].replace("\"", "");
    }
}
