// src/main/java/com/event/recruitment/intelligent_recruitment_system/model/entity/location/Location.java
package com.event.recruitment.intelligent_recruitment_system.model.entity.location;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "locations",
        indexes = {
                @Index(name = "idx_lat_lng", columnList = "latitude, longitude"),
                @Index(name = "idx_city", columnList = "city"),
                @Index(name = "idx_state", columnList = "state")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", nullable = false, length = 100, columnDefinition = "varchar(100) default 'Malaysia'")
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "place_id", length = 100, unique = true)
    private String placeId;

    // Read-only field for google_maps_url generated column
    @Column(name = "google_maps_url", length = 500, insertable = false, updatable = false)
    private String googleMapsUrl;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}