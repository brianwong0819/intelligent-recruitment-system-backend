// src/main/java/com/event/recruitment/intelligent_recruitment_system/dto/request/location/SearchLocationRequest.java
package com.event.recruitment.intelligent_recruitment_system.dto.request.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchLocationRequest {
    private String keyword;
    private String placeId;
    private Double latitude;
    private Double longitude;
    private Double radius; // Search radius in kilometers
    private String city;
    private String state;
}