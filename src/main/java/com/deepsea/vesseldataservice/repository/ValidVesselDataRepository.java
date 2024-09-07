package com.deepsea.vesseldataservice.repository;

import com.deepsea.vesseldataservice.model.ValidVesselData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ValidVesselDataRepository extends JpaRepository<ValidVesselData, String> {

}
