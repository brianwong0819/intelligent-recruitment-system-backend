// src/main/java/com/event/recruitment/intelligent_recruitment_system/service/location/LocationService.java
package com.event.recruitment.intelligent_recruitment_system.service.location;

import com.event.recruitment.intelligent_recruitment_system.config.GoogleMapsConfig;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.location.CreateLocationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.location.SearchLocationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.location.LocationResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.location.Location;
import com.event.recruitment.intelligent_recruitment_system.repository.location.LocationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final LocationRepository locationRepository;
    private final RestTemplate restTemplate;
    private final GoogleMapsConfig googleMapsConfig;

    @PersistenceContext
    private EntityManager entityManager;

    private static final double EARTH_RADIUS_KM = 6371.0;

    public Response<LocationResponseDTO> createLocation(CreateLocationRequest request) {
        try {
            // Check if a location with the same Place ID already exists
            if (request.getPlaceId() != null) {
                Optional<Location> existingLocation = locationRepository.findByPlaceId(request.getPlaceId());
                if (existingLocation.isPresent()) {
                    return new Response<>(200, "Location already exists", convertToDto(existingLocation.get()));
                }
            }

            // Create new location
            Location location = Location.builder()
                    .name(request.getName())
                    .address(request.getAddress())
                    .city(request.getCity())
                    .state(request.getState())
                    .country(request.getCountry())
                    .postalCode(request.getPostalCode())
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .placeId(request.getPlaceId())
                    .build();

            Location savedLocation = locationRepository.save(location);
            return new Response<>(201, "Location created successfully", convertToDto(savedLocation));

        } catch (Exception e) {
            log.error("Error creating location: {}", e.getMessage(), e);
            return new Response<>(500, "Error creating location: " + e.getMessage(), null);
        }
    }

    public Response<LocationResponseDTO> getLocationById(Long id) {
        try {
            Optional<Location> locationOpt = locationRepository.findById(id);
            if (locationOpt.isPresent()) {
                return new Response<>(200, "Location found", convertToDto(locationOpt.get()));
            } else {
                return new Response<>(404, "Location not found", null);
            }
        } catch (Exception e) {
            log.error("Error getting location by ID: {}", e.getMessage(), e);
            return new Response<>(500, "Error getting location: " + e.getMessage(), null);
        }
    }

    public Response<List<LocationResponseDTO>> searchLocations(SearchLocationRequest request) {
        try {
            List<Location> locations = new ArrayList<>();

            // Search by place ID if provided
            if (request.getPlaceId() != null && !request.getPlaceId().isEmpty()) {
                Optional<Location> locationOpt = locationRepository.findByPlaceId(request.getPlaceId());
                locationOpt.ifPresent(locations::add);
            }
            // Search by coordinates and radius if provided
            else if (request.getLatitude() != null && request.getLongitude() != null && request.getRadius() != null) {
                locations = findLocationsByCoordinatesAndRadius(
                        BigDecimal.valueOf(request.getLatitude()),
                        BigDecimal.valueOf(request.getLongitude()),
                        request.getRadius());
            }
            // Search by city if provided
            else if (request.getCity() != null && !request.getCity().isEmpty()) {
                locations = locationRepository.findByCity(request.getCity());
            }
            // Search by state if provided
            else if (request.getState() != null && !request.getState().isEmpty()) {
                locations = locationRepository.findByState(request.getState());
            }
            // Search by keyword if provided
            else if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                String keyword = request.getKeyword().trim();
                boolean shouldTryGoogleAPI = false;

                // 1. First try exact keyword search
                locations = locationRepository.searchByKeyword(keyword);

                // 2. If keyword contains spaces, try multi-word search
                if (keyword.contains(" ")) {
                    String[] words = keyword.split("\\s+");

                    // Filter out very short words before querying the database
                    List<String> validWords = Arrays.stream(words)
                            .filter(w -> w.length() >= 2)
                            .collect(Collectors.toList());

                    if (!validWords.isEmpty()) {
                        try {
                            // Try to find locations matching all words (AND condition)
                            List<Location> multiWordResults = searchLocationsByMultipleWords(validWords.toArray(new String[0]));

                            // If multi-word search returned results, check quality of matches
                            if (!multiWordResults.isEmpty()) {
                                // Evaluate result quality - check if each result contains all keywords
                                List<Location> qualityResults = filterResultsByKeywordQuality(multiWordResults, validWords);

                                // If high quality results exist, use them
                                if (!qualityResults.isEmpty()) {
                                    locations = qualityResults;
                                } else {
                                    // Otherwise note quality is insufficient, try Google API later
                                    shouldTryGoogleAPI = true;
                                    // Keep original results in case Google API fails
                                }
                            } else {
                                // No multi-word matches, try individual word search
                                log.debug("No results with AND search, trying OR search with individual words");

                                Set<Location> locationSet = new HashSet<>();
                                for (String word : validWords) {
                                    List<Location> wordResults = locationRepository.searchByKeyword(word);
                                    locationSet.addAll(wordResults);
                                }

                                if (!locationSet.isEmpty()) {
                                    // Single word search has results, but should try Google API for more precise matches
                                    shouldTryGoogleAPI = true;
                                    locations = new ArrayList<>(locationSet);
                                } else {
                                    // No results at all, definitely try Google API
                                    shouldTryGoogleAPI = true;
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Error during advanced multi-word search: {}", e.getMessage());
                            shouldTryGoogleAPI = true;
                        }
                    }
                }

                // 3. If we should try Google Places API (based on analysis above)
                if (shouldTryGoogleAPI || locations.isEmpty()) {
                    Response<List<LocationResponseDTO>> googleResults = searchGooglePlaces(keyword);
                    if (googleResults.getStatusCode() == 200 && googleResults.getData() != null
                            && !googleResults.getData().isEmpty()) {
                        return googleResults;
                    }
                }
            }

            List<LocationResponseDTO> results = locations.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            // Calculate distance from user if coordinates provided
            if (request.getLatitude() != null && request.getLongitude() != null) {
                results.forEach(dto -> {
                    double distance = calculateDistance(
                            request.getLatitude(), request.getLongitude(),
                            dto.getLatitude().doubleValue(), dto.getLongitude().doubleValue());
                    dto.setDistanceFromUser(distance);
                });
            }

            return new Response<>(200, "Locations found: " + results.size(), results);

        } catch (Exception e) {
            log.error("Error searching locations: {}", e.getMessage(), e);
            return new Response<>(500, "Error searching locations: " + e.getMessage(), null);
        }
    }

    /**
     * Evaluate search result quality by checking if each result contains all keywords
     * @param locations Search results
     * @param keywords Search keywords
     * @return Filtered high-quality results
     */
    private List<Location> filterResultsByKeywordQuality(List<Location> locations, List<String> keywords) {
        return locations.stream()
                .filter(location -> {
                    String combinedText = (location.getName() + " " +
                            (location.getAddress() != null ? location.getAddress() : "") + " " +
                            (location.getCity() != null ? location.getCity() : "")).toLowerCase();

                    // Check if all keywords are contained in the combined text
                    return keywords.stream()
                            .allMatch(keyword -> combinedText.contains(keyword.toLowerCase()));
                })
                .collect(Collectors.toList());
    }

    /**
     * Search for locations matching all words in the search query (AND condition)
     * This method dynamically builds a JPQL query to match all words across name, address, and city fields
     *
     * @param words Array of search terms to match
     * @return List of locations matching all search terms
     */
    private List<Location> searchLocationsByMultipleWords(String[] words) {
        if (words == null || words.length == 0) {
            return new ArrayList<>();
        }

        // Filter out very short words
        List<String> validWords = Arrays.stream(words)
                .filter(w -> w.length() >= 2) // Only use words with at least 2 characters
                .collect(Collectors.toList());

        if (validWords.isEmpty()) {
            return new ArrayList<>();
        }

        // Build a dynamic query with an AND condition for all words
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT l FROM Location l WHERE ");

        for (int i = 0; i < validWords.size(); i++) {
            if (i > 0) {
                queryBuilder.append(" AND ");
            }

            queryBuilder.append("(LOWER(l.name) LIKE :word").append(i)
                    .append(" OR LOWER(l.address) LIKE :word").append(i)
                    .append(" OR LOWER(l.city) LIKE :word").append(i)
                    .append(")");
        }

        // Create the query
        Query query = entityManager.createQuery(queryBuilder.toString(), Location.class);

        // Set parameters with wildcard pattern
        for (int i = 0; i < validWords.size(); i++) {
            query.setParameter("word" + i, "%" + validWords.get(i).toLowerCase() + "%");
        }

        // Execute and return results
        return query.getResultList();
    }

    @Cacheable(value = "locationsByPlaceId", key = "#placeId", unless = "#result.data == null")
    public Response<LocationResponseDTO> findOrCreateByPlaceId(String placeId) {
        try {
            // First check if we already have this place ID in our database
            Optional<Location> existingLocation = locationRepository.findByPlaceId(placeId);
            if (existingLocation.isPresent()) {
                return new Response<>(200, "Location found in database", convertToDto(existingLocation.get()));
            }

            // Fetch details from Google Places API
            String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/place/details/json")
                    .queryParam("place_id", placeId)
                    .queryParam("fields", "name,formatted_address,geometry,address_component")
                    .queryParam("key", googleMapsConfig.getApiKey())
                    .build()
                    .toUriString();

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && "OK".equals(responseBody.get("status"))) {
                Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
                Map<String, Object> geometry = (Map<String, Object>) result.get("geometry");
                Map<String, Object> location = (Map<String, Object>) geometry.get("location");

                String name = (String) result.get("name");
                String formattedAddress = (String) result.get("formatted_address");
                Double lat = (Double) location.get("lat");
                Double lng = (Double) location.get("lng");

                // Parse address components to extract city, state, etc.
                String city = "";
                String state = "";
                String country = "Malaysia"; // Default country
                String postalCode = "";

                List<Map<String, Object>> addressComponents = (List<Map<String, Object>>) result.get("address_components");
                if (addressComponents != null) {
                    for (Map<String, Object> component : addressComponents) {
                        List<String> types = (List<String>) component.get("types");
                        String longName = (String) component.get("long_name");

                        if (types.contains("locality")) {
                            city = longName;
                        } else if (types.contains("administrative_area_level_1")) {
                            state = longName;
                        } else if (types.contains("country")) {
                            country = longName;
                        } else if (types.contains("postal_code")) {
                            postalCode = longName;
                        }
                    }
                }

                // Create a new location
                CreateLocationRequest createRequest = CreateLocationRequest.builder()
                        .name(name)
                        .address(formattedAddress)
                        .city(city)
                        .state(state)
                        .country(country)
                        .postalCode(postalCode)
                        .latitude(BigDecimal.valueOf(lat))
                        .longitude(BigDecimal.valueOf(lng))
                        .placeId(placeId)
                        .build();

                return createLocation(createRequest);
            } else {
                log.error("Error fetching place details from Google API: {}", responseBody != null ? responseBody.get("status") : "null response");
                return new Response<>(404, "Location not found in Google Places API", null);
            }
        } catch (Exception e) {
            log.error("Error in findOrCreateByPlaceId: {}", e.getMessage(), e);
            return new Response<>(500, "Error finding or creating location: " + e.getMessage(), null);
        }
    }

    public Response<List<LocationResponseDTO>> findNearbyLocations(BigDecimal latitude, BigDecimal longitude, Double radiusKm) {
        try {
            List<Location> locations = findLocationsByCoordinatesAndRadius(latitude, longitude, radiusKm);

            List<LocationResponseDTO> results = locations.stream()
                    .map(location -> {
                        LocationResponseDTO dto = convertToDto(location);
                        // Calculate distance
                        double distance = calculateDistance(
                                latitude.doubleValue(), longitude.doubleValue(),
                                location.getLatitude().doubleValue(), location.getLongitude().doubleValue());
                        dto.setDistanceFromUser(distance);
                        return dto;
                    })
                    .collect(Collectors.toList());

            return new Response<>(200, "Found " + results.size() + " locations within " + radiusKm + " km", results);

        } catch (Exception e) {
            log.error("Error finding nearby locations: {}", e.getMessage(), e);
            return new Response<>(500, "Error finding nearby locations: " + e.getMessage(), null);
        }
    }

    @Cacheable(value = "googlePlacesSearch", key = "#keyword", unless = "#result.data == null || #result.data.isEmpty()")
    public Response<List<LocationResponseDTO>> searchGooglePlaces(String keyword) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/place/textsearch/json")
                    .queryParam("query", keyword)
                    .queryParam("region", "my") // Biasing results towards Malaysia
                    .queryParam("key", googleMapsConfig.getApiKey())
                    .build()
                    .toUriString();

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && "OK".equals(responseBody.get("status"))) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("results");
                List<LocationResponseDTO> locationResults = new ArrayList<>();

                for (Map<String, Object> result : results) {
                    Map<String, Object> geometry = (Map<String, Object>) result.get("geometry");
                    Map<String, Object> location = (Map<String, Object>) geometry.get("location");

                    String name = (String) result.get("name");
                    String formattedAddress = (String) result.get("formatted_address");
                    String placeId = (String) result.get("place_id");
                    Double lat = (Double) location.get("lat");
                    Double lng = (Double) location.get("lng");

                    // Parse the formatted address to extract components
                    String[] addressParts = formattedAddress.split(",");
                    String city = addressParts.length > 1 ? addressParts[addressParts.length - 2].trim() : "";
                    String country = addressParts.length > 0 ? addressParts[addressParts.length - 1].trim() : "Malaysia";

                    LocationResponseDTO dto = LocationResponseDTO.builder()
                            .name(name)
                            .address(formattedAddress)
                            .city(city)
                            .country(country)
                            .latitude(BigDecimal.valueOf(lat))
                            .longitude(BigDecimal.valueOf(lng))
                            .placeId(placeId)
                            .googleMapsUrl("https://www.google.com/maps/place/?q=place_id:" + placeId)
                            .build();

                    locationResults.add(dto);
                }

                return new Response<>(200, "Found " + locationResults.size() + " locations from Google Places API", locationResults);
            } else {
                log.error("Error searching Google Places API: {}", responseBody != null ? responseBody.get("status") : "null response");
                return new Response<>(404, "No locations found in Google Places API", new ArrayList<>());
            }
        } catch (Exception e) {
            log.error("Error searching Google Places: {}", e.getMessage(), e);
            return new Response<>(500, "Error searching Google Places: " + e.getMessage(), null);
        }
    }

    public LocationResponseDTO convertToDto(Location location) {
        return LocationResponseDTO.builder()
                .id(location.getId())
                .name(location.getName())
                .address(location.getAddress())
                .city(location.getCity())
                .state(location.getState())
                .country(location.getCountry())
                .postalCode(location.getPostalCode())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .placeId(location.getPlaceId())
                .googleMapsUrl(location.getGoogleMapsUrl())
                .build();
    }

    // Helper method to find locations within a radius
    private List<Location> findLocationsByCoordinatesAndRadius(BigDecimal latitude, BigDecimal longitude, Double radiusKm) {
        // Calculate the approximate bounding box for the given radius
        // This is a simple approximation that doesn't account for the Earth's curvature at high latitudes
        double latDegreePerKm = 1.0 / 110.574; // 1 degree of latitude is approximately 110.574 km
        double lngDegreePerKm = 1.0 / (111.320 * Math.cos(Math.toRadians(latitude.doubleValue()))); // Longitude degree distance varies with latitude

        double latOffset = radiusKm * latDegreePerKm;
        double lngOffset = radiusKm * lngDegreePerKm;

        BigDecimal minLat = latitude.subtract(BigDecimal.valueOf(latOffset));
        BigDecimal maxLat = latitude.add(BigDecimal.valueOf(latOffset));
        BigDecimal minLng = longitude.subtract(BigDecimal.valueOf(lngOffset));
        BigDecimal maxLng = longitude.add(BigDecimal.valueOf(lngOffset));

        // First, get all locations in the bounding box (this is faster than calculating distance for every location)
        List<Location> locationsInBox = locationRepository.findByCoordinateRange(minLat, maxLat, minLng, maxLng);

        // Then filter by precise distance
        return locationsInBox.stream()
                .filter(location -> {
                    double distance = calculateDistance(
                            latitude.doubleValue(), longitude.doubleValue(),
                            location.getLatitude().doubleValue(), location.getLongitude().doubleValue());
                    return distance <= radiusKm;
                })
                .collect(Collectors.toList());
    }

    // Calculate distance between two points using Haversine formula
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}