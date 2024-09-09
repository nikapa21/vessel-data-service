package com.deepsea.vesseldataservice.service;

import static java.util.Objects.nonNull;

import com.deepsea.vesseldataservice.exception.DataNotFoundException;
import com.deepsea.vesseldataservice.model.ValidVesselData;
import com.deepsea.vesseldataservice.repository.InvalidVesselDataRepository;
import com.deepsea.vesseldataservice.repository.ValidVesselDataRepository;
import com.deepsea.vesseldataservice.response.InvalidReasonResponse;
import com.deepsea.vesseldataservice.response.SpeedDifferenceResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VesselDataService {

    private final ValidVesselDataRepository validVesselDataRepository;
    private final InvalidVesselDataRepository invalidVesselDataRepository;

    public VesselDataService(ValidVesselDataRepository validVesselDataRepository, InvalidVesselDataRepository invalidVesselDataRepository) {

        this.validVesselDataRepository = validVesselDataRepository;
        this.invalidVesselDataRepository = invalidVesselDataRepository;
    }

    public List<SpeedDifferenceResponse> calculateSpeedDifference(String vesselCode, String latitude, String longitude) {

        List<ValidVesselData> vesselDataList;

        if (nonNull(latitude) && nonNull(longitude)) {
            vesselDataList = validVesselDataRepository.findByVesselCodeAndLatitudeAndLongitude(vesselCode, latitude, longitude);
        } else {
            vesselDataList = validVesselDataRepository.findByVesselCode(vesselCode);
        }

        if (vesselDataList.isEmpty()) {
            throw new DataNotFoundException("No data found for the given vessel code and coordinates.");
        }

        return vesselDataList.stream()
                .map(data -> new SpeedDifferenceResponse(
                        data.getLatitude(),
                        data.getLongitude(),
                        data.getSpeedDifference()))
                .toList();
    }

    public List<InvalidReasonResponse> getInvalidReasonsByVesselCode(String vesselCode) {

        List<Object[]> results = invalidVesselDataRepository.findInvalidReasonsByVesselCode(vesselCode);
        if (results.isEmpty()) {
            throw new DataNotFoundException("No invalid data found for vessel code: " + vesselCode);
        }
        return results.stream()
                .filter(result -> nonNull(result[0]) && (Long) result[1] > 0)
                .map(result -> new InvalidReasonResponse((String) result[0], (Long) result[1]))
                .toList();
    }

    public String compareVesselCompliance(String vesselCode1, String vesselCode2) {

        var compliancePercentage1 = calculateOverallCompliance(vesselCode1);
        var compliancePercentage2 = calculateOverallCompliance(vesselCode2);

        return switch (Double.compare(compliancePercentage1, compliancePercentage2)) {
            case 1 -> "Vessel " + vesselCode1 + " is more compliant with a compliance percentage of " + compliancePercentage1 + ".";
            case -1 -> "Vessel " + vesselCode2 + " is more compliant with a compliance percentage of " + compliancePercentage2 + ".";
            default -> "Both vessels have the same compliance percentage of " + compliancePercentage1 + ".";
        };
    }

    public double calculateOverallCompliance(String vesselCode) {

        return validVesselDataRepository.calculateOverallComplianceByVesselCode(vesselCode);
    }

    public List<ValidVesselData> getVesselDataForPeriod(String vesselCode, String startDate, String endDate) {

        var vesselDataList = validVesselDataRepository.findByVesselCodeAndDateRange(vesselCode, startDate, endDate);

        if (vesselDataList.isEmpty()) {
            throw new DataNotFoundException("No data found for vessel code: " + vesselCode + " in the specified period.");
        }
        return vesselDataList;
    }
}