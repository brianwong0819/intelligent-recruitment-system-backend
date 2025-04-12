// src/main/java/com/event/recruitment/intelligent_recruitment_system/dto/response/location/LocationResponseDTO.java
package com.event.recruitment.intelligent_recruitment_system.dto.response.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponseDTO {
    private Long id;
    private String name;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String placeId;
    private String googleMapsUrl;

    // Additional fields for frontend convenience
    private Double distanceFromUser; // In kilometers, calculated if user provides coordinates
}