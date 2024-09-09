package com.deepsea.vesseldataservice.repository;

import com.deepsea.vesseldataservice.model.ValidVesselData;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ValidVesselDataRepository extends JpaRepository<ValidVesselData, String> {

    List<ValidVesselData> findByVesselCode(String vesselCode);

    List<ValidVesselData> findByVesselCodeAndLatitudeAndLongitude(String vesselCode, String latitude, String longitude);

    @Query("SELECT v FROM ValidVesselData v WHERE v.vesselCode = :vesselCode AND v.datetime BETWEEN :startDate AND :endDate")
    List<ValidVesselData> findByVesselCodeAndDateRange(String vesselCode, String startDate, String endDate);

    @Query("SELECT AVG(compliancePercentage) FROM ValidVesselData WHERE vesselCode = :vesselCode")
    Double calculateOverallComplianceByVesselCode(String vesselCode);
}
