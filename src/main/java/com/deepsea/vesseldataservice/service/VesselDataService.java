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
        logger.debug("VesselDataService instantiated with repositories.");
    }

    public List<SpeedDifferenceResponse> calculateSpeedDifference(String vesselCode, String latitude, String longitude) {

        logger.debug("Calculating speed difference for vesselCode: {}, latitude: {}, longitude: {}", vesselCode, latitude, longitude);

        List<ValidVesselData> vesselDataList;

        if (nonNull(latitude) && nonNull(longitude)) {
            vesselDataList = validVesselDataRepository.findByVesselCodeAndLatitudeAndLongitude(vesselCode, latitude, longitude);
        } else {
            vesselDataList = validVesselDataRepository.findByVesselCode(vesselCode);
        }

        if (vesselDataList.isEmpty()) {
            logger.warn("No data found for vessel code: {} and coordinates: {}, {}", vesselCode, latitude, longitude);
            throw new DataNotFoundException("No data found for the given vessel code and coordinates.");
        }

        logger.debug("Found {} valid vessel data entries.", vesselDataList.size());
        return vesselDataList.stream()
                .map(data -> new SpeedDifferenceResponse(
                        data.getLatitude(),
                        data.getLongitude(),
                        data.getSpeedDifference()))
                .toList();
    }

    public Page<SpeedDifferenceResponse> calculateSpeedDifferences(String vesselCode, Pageable pageable) {

        logger.debug("Calculating speed differences for vesselCode: {} with pagination: {}", vesselCode, pageable);

        Page<ValidVesselData> page = validVesselDataRepository.findByVesselCode(vesselCode, pageable);
        logger.debug("Found {} valid vessel data entries for vesselCode: {}", page.getTotalElements(), vesselCode);

        return page.map(data -> new SpeedDifferenceResponse(
                data.getLatitude(),
                data.getLongitude(),
                data.getSpeedDifference())
        );
    }

    public List<InvalidReasonResponse> getInvalidReasonsByVesselCode(String vesselCode) {

        logger.debug("Fetching invalid reasons for vesselCode: {}", vesselCode);

        List<Object[]> results = invalidVesselDataRepository.findInvalidReasonsByVesselCode(vesselCode);
        if (results.isEmpty()) {
            logger.warn("No invalid data found for vessel code: {}", vesselCode);
            throw new DataNotFoundException("No invalid data found for vessel code: " + vesselCode);
        }

        logger.debug("Found {} invalid reasons for vesselCode: {}", results.size(), vesselCode);
        return results.stream()
                .filter(result -> nonNull(result[0]) && (Long) result[1] > 0)
                .map(result -> new InvalidReasonResponse((String) result[0], (Long) result[1]))
                .toList();
    }

    public String compareVesselCompliance(String vesselCode1, String vesselCode2) {

        logger.debug("Comparing compliance between vesselCode1: {} and vesselCode2: {}", vesselCode1, vesselCode2);

        var compliancePercentage1 = calculateOverallCompliance(vesselCode1);
        var compliancePercentage2 = calculateOverallCompliance(vesselCode2);

        logger.debug("Compliance percentages - {}: {}, {}: {}", vesselCode1, compliancePercentage1, vesselCode2, compliancePercentage2);

        return switch (Double.compare(compliancePercentage1, compliancePercentage2)) {
            case 1 -> "Vessel " + vesselCode1 + " is more compliant with a compliance percentage of " + compliancePercentage1 + ".";
            case -1 -> "Vessel " + vesselCode2 + " is more compliant with a compliance percentage of " + compliancePercentage2 + ".";
            default -> "Both vessels have the same compliance percentage of " + compliancePercentage1 + ".";
        };
    }

    public double calculateOverallCompliance(String vesselCode) {

        logger.debug("Calculating overall compliance for vesselCode: {}", vesselCode);

        var overallCompliance = validVesselDataRepository.calculateOverallComplianceByVesselCode(vesselCode);

        if (isNull(overallCompliance)) {
            logger.warn("No data found for vessel code: {}", vesselCode);
            throw new DataNotFoundException("No data found for vessel code: " + vesselCode);
        }

        logger.debug("Overall compliance for vesselCode {}: {}", vesselCode, overallCompliance);
        return overallCompliance;
    }

    public List<ValidVesselData> getVesselDataForPeriod(String vesselCode, String startDate, String endDate) {

        logger.debug("Fetching vessel data for vesselCode: {} from {} to {}", vesselCode, startDate, endDate);

        var vesselDataList = validVesselDataRepository.findByVesselCodeAndDateRange(vesselCode, startDate, endDate);

        if (vesselDataList.isEmpty()) {
            logger.warn("No data found for vessel code: {} in the specified period.", vesselCode);
            throw new DataNotFoundException("No data found for vessel code: " + vesselCode + " in the specified period.");
        }

        logger.debug("Found {} vessel data entries for vesselCode: {} in the specified period.", vesselDataList.size(), vesselCode);
        return vesselDataList;
    }

    public List<ProblemGroup> identifyProblematicData(String vesselCode, String invalidReason, Long overrideIntervalValue, Integer sizeThreshold) {

        logger.debug("Identifying problematic data for vesselCode: {}, invalidReason: {}, overrideIntervalValue: {}, sizeThreshold: {}", vesselCode, invalidReason, overrideIntervalValue, sizeThreshold);

        List<ProblemGroup> results = new ArrayList<>();

        if (nonNull(overrideIntervalValue)) {
            this.minutesIntervalForConsecutiveGroups = overrideIntervalValue;
            logger.debug("Override interval value set to: {}", overrideIntervalValue);
        }

        List<InvalidVesselData> invalidDataList;

        if ("all".equals(invalidReason)) {
            invalidDataList = invalidVesselDataRepository.findByVesselCode(vesselCode);
        } else {
            invalidDataList = invalidVesselDataRepository.findByVesselCodeAndInvalidReason(vesselCode, invalidReason);
        }

        if (invalidDataList.isEmpty()) {
            logger.warn("No invalid data found for vessel code: {}", vesselCode);
            throw new DataNotFoundException("No invalid data found for vessel code: " + vesselCode);
        }

        logger.debug("Found {} invalid data entries for vesselCode: {}", invalidDataList.size(), vesselCode);

        // Identify groups of consecutive waypoints (not sorted)
        List<List<InvalidVesselData>> unsortedGroupsOfConsecutiveWaypoints = identifyGroupsOfConsecutiveWaypoints(invalidDataList);

        for (List<InvalidVesselData> invalidVesselDataList : unsortedGroupsOfConsecutiveWaypoints) {
            ProblemGroup problemGroup = new ProblemGroup();

            problemGroup.setSize(invalidVesselDataList.size());
            problemGroup.setStartDateTime(invalidVesselDataList.getFirst().getDatetime());
            problemGroup.setEndDateTime(invalidVesselDataList.getLast().getDatetime());

            if (invalidVesselDataList.size() > sizeThreshold) {
                results.add(problemGroup);
                logger.debug("Added problem group of size {} for vesselCode: {}", invalidVesselDataList.size(), vesselCode);
            }
        }

        results.sort(Comparator.comparingInt(ProblemGroup::getSize).reversed());
        logger.debug("Returning {} problem groups for vesselCode: {}", results.size(), vesselCode);
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
                logger.debug("Creating a new group of anomalies ");
                group = new ArrayList<>();
                group.add(currentVesselData);
                last = currentVesselData;
                result.add(group);
            }
        }
        logger.debug("Returning a list of size {} groups ", result.size());
        return result;
    }

    boolean belongsToSameGroup(String datetime, String currentDateTime) {

        //convert datetime to java dates
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Convert the string to LocalDateTime
        LocalDateTime dateTime = LocalDateTime.parse(datetime, formatter);
        LocalDateTime currentLocalDateTime = LocalDateTime.parse(currentDateTime, formatter);
        boolean belongs = currentLocalDateTime.isBefore(dateTime.plusMinutes(minutesIntervalForConsecutiveGroups));
        logger.debug("Checking if {} belongs to the same group as {}: {}", currentDateTime, datetime, belongs);
        return belongs;
    }
}