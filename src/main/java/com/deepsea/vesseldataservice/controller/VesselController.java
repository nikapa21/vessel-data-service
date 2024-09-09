package com.deepsea.vesseldataservice.controller;

import com.deepsea.vesseldataservice.exception.DataNotFoundException;
import com.deepsea.vesseldataservice.model.ValidVesselData;
import com.deepsea.vesseldataservice.response.InvalidReasonResponse;
import com.deepsea.vesseldataservice.response.SpeedDifferenceResponse;
import com.deepsea.vesseldataservice.service.CsvService;
import com.deepsea.vesseldataservice.service.VesselDataService;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vessels")
public class VesselController {

    private final CsvService csvService;
    private final VesselDataService vesselDataService;

    public VesselController(CsvService csvService, VesselDataService vesselDataService) {

        this.csvService = csvService;
        this.vesselDataService = vesselDataService;
    }

    @GetMapping("/hello")
    public String sayHello() {

        csvService.readCsvInChunks();
        return "Data processing and insertion completed!";
    }

    @GetMapping("/{vesselCode}/speed-difference")
    public ResponseEntity<List<SpeedDifferenceResponse>> getSpeedDifference(
            @PathVariable String vesselCode,
            @RequestParam(required = false) @Pattern(regexp = "^-?\\d+(\\.\\d+)?$", message = "Invalid latitude format") String latitude,
            @RequestParam(required = false) @Pattern(regexp = "^-?\\d+(\\.\\d+)?$", message = "Invalid longitude format") String longitude) {

        List<SpeedDifferenceResponse> speedDifferences = vesselDataService.calculateSpeedDifference(vesselCode, latitude, longitude);
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

        try {
            var result = vesselDataService.compareVesselCompliance(vesselCode1, vesselCode2);
            return ResponseEntity.ok(result);
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error comparing vessel compliance.");
        }
    }

    @GetMapping("/{vesselCode}/data")
    public ResponseEntity<List<ValidVesselData>> getVesselDataForPeriod(
            @PathVariable String vesselCode,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        try {
            var vesselData = vesselDataService.getVesselDataForPeriod(vesselCode, startDate, endDate);
            return ResponseEntity.ok(vesselData);
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}