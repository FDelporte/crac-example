package be.webtechie.crac.example.model;

import java.util.List;
import java.util.stream.Collectors;

public record DataSet(String fileName, List<DataRecord> records) {
    public String toCsv() {
        return records.stream()
                .limit(100)
                .map(DataRecord::toCsv)
                .collect(Collectors.joining("\n"));
    }
}
