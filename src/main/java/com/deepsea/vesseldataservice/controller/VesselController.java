package com.deepsea.vesseldataservice.controller;

import com.deepsea.vesseldataservice.model.ProblemGroup;
import com.deepsea.vesseldataservice.model.ValidVesselData;
import com.deepsea.vesseldataservice.response.InvalidReasonResponse;
import com.deepsea.vesseldataservice.response.SpeedDifferenceResponse;
import com.deepsea.vesseldataservice.service.CsvService;
import com.deepsea.vesseldataservice.service.VesselDataService;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/vessels")
public class VesselController {

    private final CsvService csvService;
    private final VesselDataService vesselDataService;

    public VesselController(CsvService csvService, VesselDataService vesselDataService) {

        this.csvService = csvService;
        this.vesselDataService = vesselDataService;
    }

    @GetMapping("/processFile")
    public ResponseEntity<String> processFile() {

        csvService.readCsvInChunks();
        return ResponseEntity.ok("Data processing and insertion completed!");
    }

    @GetMapping("/{vesselCode}/speed-difference")
    public ResponseEntity<List<SpeedDifferenceResponse>> getSpeedDifference(
            @PathVariable String vesselCode,
            @RequestParam @Pattern(regexp = "^-?\\d+(\\.\\d+)?$", message = "Invalid latitude format") String latitude,
            @RequestParam @Pattern(regexp = "^-?\\d+(\\.\\d+)?$", message = "Invalid longitude format") String longitude) {

        List<SpeedDifferenceResponse> speedDifferences = vesselDataService.calculateSpeedDifference(vesselCode, latitude, longitude);
        return ResponseEntity.ok(speedDifferences);
    }

    @GetMapping("/{vesselCode}/speed-differences")
    public ResponseEntity<Page<SpeedDifferenceResponse>> getSpeedDifferences(
            @PathVariable String vesselCode, Pageable pageable) {

        Page<SpeedDifferenceResponse> speedDifferences = vesselDataService.calculateSpeedDifferences(vesselCode, pageable);
        return ResponseEntity.ok(speedDifferences);
    }

    @GetMapping("/{vesselCode}/invalid-reasons")
    public ResponseEntity<List<InvalidReasonResponse>> getInvalidReasons(
            @PathVariable String vesselCode) {

        List<InvalidReasonResponse> invalidReasons = vesselDataService.getInvalidReasonsByVesselCode(vesselCode);
        return ResponseEntity.ok(invalidReasons);
    }

    @GetMapping("/compare-compliance")
    public ResponseEntity<String> compareVesselCompliance(
            @RequestParam String vesselCode1,
            @RequestParam String vesselCode2) {

        String result = vesselDataService.compareVesselCompliance(vesselCode1, vesselCode2);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{vesselCode}/data")
    public ResponseEntity<List<ValidVesselData>> getVesselDataForPeriod(
            @PathVariable String vesselCode,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        var vesselData = vesselDataService.getVesselDataForPeriod(vesselCode, startDate, endDate);
        return ResponseEntity.ok(vesselData);
    }

    @GetMapping("/{vesselCode}/problems")
    public ResponseEntity<List<ProblemGroup>> getProblematicData(
            @PathVariable String vesselCode,
            @RequestParam(required = false, defaultValue = "all") String invalidReason,
            @RequestParam(required = false, defaultValue = "60") String overrideIntervalValue,
            @RequestParam(required = false, defaultValue = "10") String sizeThreshold) {

        var problemGroups = vesselDataService.identifyProblematicData(vesselCode, invalidReason, Long.valueOf(overrideIntervalValue), Integer.valueOf(sizeThreshold));
        return ResponseEntity.ok(problemGroups);
    }
}