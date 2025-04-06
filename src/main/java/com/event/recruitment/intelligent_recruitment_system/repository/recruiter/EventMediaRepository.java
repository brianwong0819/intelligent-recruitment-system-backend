package com.event.recruitment.intelligent_recruitment_system.repository.recruiter;

import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.EventMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventMediaRepository extends JpaRepository<EventMedia, Integer> {

    List<EventMedia> findByEventId(Integer eventId);

    Optional<EventMedia> findByIdAndEventId(Integer id, Integer eventId);

    void deleteByIdAndEventId(Integer id, Integer eventId);
}