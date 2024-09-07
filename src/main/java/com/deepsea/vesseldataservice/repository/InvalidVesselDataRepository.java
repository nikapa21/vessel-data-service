package com.deepsea.vesseldataservice.repository;

import com.deepsea.vesseldataservice.model.InvalidVesselData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvalidVesselDataRepository extends JpaRepository<InvalidVesselData, String> {

}
