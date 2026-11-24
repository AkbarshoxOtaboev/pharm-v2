package uz.uwon.pharm.location;

public final class LocationDtos {
    private LocationDtos() {}

    public record RegionResponse(Long id, String name) {}

    public record DistrictResponse(Long id, String name, Long regionId, String regionName) {}

    public record NameRequest(String name, Long parentId) {}
}
