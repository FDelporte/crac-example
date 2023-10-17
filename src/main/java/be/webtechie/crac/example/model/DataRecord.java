package be.webtechie.crac.example.model;

public record DataRecord(
        long id,
        String organizationId,
        String name,
        String website,
        String country,
        String description,
        String founded,
        String industry,
        long numberOfEmployees) {
    public String toCsv() {
        return id + "," +
                organizationId + "," +
                name + "," +
                website + "," +
                country + "," +
                description + "," +
                founded + "," +
                industry + "," +
                numberOfEmployees;
    }
}
