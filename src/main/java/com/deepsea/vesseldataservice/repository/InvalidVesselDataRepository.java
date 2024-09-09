package com.deepsea.vesseldataservice.repository;

import com.deepsea.vesseldataservice.model.InvalidVesselData;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface InvalidVesselDataRepository extends JpaRepository<InvalidVesselData, String> {

    List<InvalidVesselData> findByVesselCode(String vesselCode);

    @Query("SELECT invalidReason, COUNT(invalidReason) FROM InvalidVesselData WHERE vesselCode = :vesselCode GROUP BY invalidReason ORDER BY COUNT(invalidReason) DESC")
    List<Object[]> findInvalidReasonsByVesselCode(String vesselCode);
}
