package com.deepsea.vesseldataservice.controller;

import com.deepsea.vesseldataservice.model.InvalidVesselData;
import com.deepsea.vesseldataservice.model.ValidVesselData;
import com.deepsea.vesseldataservice.repository.InvalidVesselDataRepository;
import com.deepsea.vesseldataservice.repository.ValidVesselDataRepository;
import com.deepsea.vesseldataservice.service.CsvService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VesselController {

    private final CsvService csvService;
    private final ValidVesselDataRepository validVesselDataRepository;
    private final InvalidVesselDataRepository invalidVesselDataRepository;

    public VesselController(CsvService csvService, ValidVesselDataRepository validVesselDataRepository, InvalidVesselDataRepository invalidVesselDataRepository) {

        this.csvService = csvService;
        this.validVesselDataRepository = validVesselDataRepository;
        this.invalidVesselDataRepository = invalidVesselDataRepository;
    }

    @GetMapping("/hello")
    public String sayHello() {

        csvService.readCsvInChunks();
        return "Hello!";
    }

    @GetMapping("/findAllValid")
    public List<ValidVesselData> findAllValid() {

        return validVesselDataRepository.findAll();
    }

    @GetMapping("/findAllInvalid")
    public List<InvalidVesselData> findAllInvalid() {

        return invalidVesselDataRepository.findAll();
    }
}