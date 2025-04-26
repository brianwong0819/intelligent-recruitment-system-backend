package com.event.recruitment.intelligent_recruitment_system.repository.location;

import com.event.recruitment.intelligent_recruitment_system.model.entity.location.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findByPlaceId(String placeId);

    @Query("SELECT l FROM Location l WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(l.address) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(l.city) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Location> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT l FROM Location l WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :word, '%')) " +
            "OR LOWER(l.address) LIKE LOWER(CONCAT('%', :word, '%')) " +
            "OR LOWER(l.city) LIKE LOWER(CONCAT('%', :word, '%'))")
    List<Location> searchByWord(@Param("word") String word);

    @Query(value = "SELECT * FROM locations l WHERE " +
            "(:searchTerms)::text[] <@ array[" +
            "   lower(l.name), lower(l.address), lower(l.city)" +
            "]", nativeQuery = true)
    List<Location> searchByAllTerms(@Param("searchTerms") String[] searchTerms);

    @Query("SELECT l FROM Location l WHERE " +
            "l.latitude BETWEEN :minLat AND :maxLat AND " +
            "l.longitude BETWEEN :minLng AND :maxLng")
    List<Location> findByCoordinateRange(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng);

    List<Location> findByCity(String city);

    List<Location> findByState(String state);
}