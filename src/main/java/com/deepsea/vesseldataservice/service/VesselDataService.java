package com.deepsea.vesseldataservice.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.deepsea.vesseldataservice.exception.DataNotFoundException;
import com.deepsea.vesseldataservice.model.InvalidVesselData;
import com.deepsea.vesseldataservice.model.ProblemGroup;
import com.deepsea.vesseldataservice.model.ValidVesselData;
import com.deepsea.vesseldataservice.repository.InvalidVesselDataRepository;
import com.deepsea.vesseldataservice.repository.ValidVesselDataRepository;
import com.deepsea.vesseldataservice.response.InvalidReasonResponse;
import com.deepsea.vesseldataservice.response.SpeedDifferenceResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class VesselDataService {

    private static final Logger logger = LoggerFactory.getLogger(VesselDataService.class);

    long minutesIntervalForConsecutiveGroups = 60;

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

    public Page<SpeedDifferenceResponse> calculateSpeedDifferences(String vesselCode, Pageable pageable) {

        Page<ValidVesselData> page = validVesselDataRepository.findByVesselCode(vesselCode, pageable);

        return page.map(data -> new SpeedDifferenceResponse(
                data.getLatitude(),
                data.getLongitude(),
                data.getSpeedDifference())
        );
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

        var overallCompliance = validVesselDataRepository.calculateOverallComplianceByVesselCode(vesselCode);

        if (isNull(overallCompliance)) {
            throw new DataNotFoundException("No data found for vessel code: " + vesselCode);
        }
        return overallCompliance;
    }

    public List<ValidVesselData> getVesselDataForPeriod(String vesselCode, String startDate, String endDate) {

        var vesselDataList = validVesselDataRepository.findByVesselCodeAndDateRange(vesselCode, startDate, endDate);

        if (vesselDataList.isEmpty()) {
            throw new DataNotFoundException("No data found for vessel code: " + vesselCode + " in the specified period.");
        }
        return vesselDataList;
    }

    public List<ProblemGroup> identifyProblematicData(String vesselCode, String invalidReason, Long overrideIntervalValue, Integer sizeThreshold) {

        List<ProblemGroup> results = new ArrayList<>();

        if (nonNull(overrideIntervalValue)) {
            this.minutesIntervalForConsecutiveGroups = overrideIntervalValue;
        }

        List<InvalidVesselData> invalidDataList;

        if ("all".equals(invalidReason)) {
            invalidDataList = invalidVesselDataRepository.findByVesselCode(vesselCode);
        } else {
            invalidDataList = invalidVesselDataRepository.findByVesselCodeAndInvalidReason(vesselCode, invalidReason);
        }

        if (invalidDataList.isEmpty()) {
            throw new DataNotFoundException("No invalid data found for vessel code: " + vesselCode);
        }

        //identify groups of consecutive waypoints (not sorted)
        List<List<InvalidVesselData>> unsortedGroupsOfConsecutiveWaypoints = identifyGroupsOfConsecutiveWaypoints(invalidDataList);

        for (List<InvalidVesselData> invalidVesselDataList : unsortedGroupsOfConsecutiveWaypoints) {

            ProblemGroup problemGroup = new ProblemGroup();

            problemGroup.setSize(invalidVesselDataList.size());
            problemGroup.setStartDateTime(invalidVesselDataList.getFirst().getDatetime());
            problemGroup.setEndDateTime(invalidVesselDataList.getLast().getDatetime());

            if (invalidVesselDataList.size() > sizeThreshold) {
                results.add(problemGroup);
            }
        }

        results.sort(Comparator.comparingInt(ProblemGroup::getSize).reversed());

        return results;
    }

    private List<List<InvalidVesselData>> identifyGroupsOfConsecutiveWaypoints(List<InvalidVesselData> invalidDataList) {

        List<List<InvalidVesselData>> result = new ArrayList<>();
        List<InvalidVesselData> group = new ArrayList<>();
        InvalidVesselData last = null;

        for (InvalidVesselData currentVesselData : invalidDataList) {
            if (last == null) {
                last = currentVesselData;
                group.add(currentVesselData);
                result.add(group);
            } else if (belongsToSameGroup(last.getDatetime(), currentVesselData.getDatetime())) {
                last = currentVesselData;
                group.add(currentVesselData);
            } else {
                logger.info("Creating a new group of anomalies ");
                group = new ArrayList<>();
                group.add(currentVesselData);
                last = currentVesselData;
                result.add(group);
            }
        }
        logger.info("Returning a list of size {} groups ", result.size());
        return result;
    }

    boolean belongsToSameGroup(String datetime, String currentDateTime) {

        //convert datetime to java dates
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Convert the string to LocalDateTime
        LocalDateTime dateTime = LocalDateTime.parse(datetime, formatter);
        LocalDateTime currentLocalDateTime = LocalDateTime.parse(currentDateTime, formatter);
        return currentLocalDateTime.isBefore(dateTime.plusMinutes(minutesIntervalForConsecutiveGroups));
    }
}