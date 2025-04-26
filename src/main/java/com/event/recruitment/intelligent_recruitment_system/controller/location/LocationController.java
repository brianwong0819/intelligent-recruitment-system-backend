package com.event.recruitment.intelligent_recruitment_system.controller.location;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.location.CreateLocationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.location.SearchLocationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.location.LocationResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.service.location.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Slf4j
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<LocationResponseDTO>> createLocation(
            @Valid @RequestBody CreateLocationRequest request) {
        log.info("Creating new location: {}", request.getName());
        Response<LocationResponseDTO> response = locationService.createLocation(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<LocationResponseDTO>> getLocationById(@PathVariable Long id) {
        log.info("Getting location with ID: {}", id);
        Response<LocationResponseDTO> response = locationService.getLocationById(id);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/search")
    public ResponseEntity<Response<List<LocationResponseDTO>>> searchLocations(
            @Valid @RequestBody SearchLocationRequest request) {
        log.info("Searching locations with criteria: {}", request);
        Response<List<LocationResponseDTO>> response = locationService.searchLocations(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/place/{placeId}")
    public ResponseEntity<Response<LocationResponseDTO>> findOrCreateByPlaceId(
            @PathVariable String placeId) {
        log.info("Finding or creating location by Place ID: {}", placeId);
        Response<LocationResponseDTO> response = locationService.findOrCreateByPlaceId(placeId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/nearby")
    public ResponseEntity<Response<List<LocationResponseDTO>>> findNearbyLocations(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(defaultValue = "10.0") Double radius) {
        log.info("Finding locations near ({}, {}) within {} km", latitude, longitude, radius);
        Response<List<LocationResponseDTO>> response = locationService.findNearbyLocations(latitude, longitude, radius);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/google-search")
    public ResponseEntity<Response<List<LocationResponseDTO>>> searchGooglePlaces(
            @RequestParam String keyword) {
        log.info("Searching Google Places API for: {}", keyword);
        Response<List<LocationResponseDTO>> response = locationService.searchGooglePlaces(keyword);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}