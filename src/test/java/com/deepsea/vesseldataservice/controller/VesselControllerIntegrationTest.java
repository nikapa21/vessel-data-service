package com.deepsea.vesseldataservice.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Allows non-static @BeforeAll
class VesselControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    void whenGetRequestToProcessFile_thenCorrectResponse() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.get("/api/vessels/processFile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().string("Data processing and insertion completed!"));
    }

    @Test
    void whenGetRequestToSpeedDifferences_thenCorrectResponse() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.get("/api/vessels/3001/speed-differences")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$['pageable']['paged']").value("true"));
    }

    @Test
    void whenGetRequestToSpeedDifferencesPageZero_thenCorrectResponses() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.get("/api/vessels/3001/speed-differences")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].latitude").value("44.8565368652344"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].longitude").value("-51.4986343383789"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].speedDifference").value(-1.109869775917801))
                .andExpect(MockMvcResultMatchers.jsonPath("$['number']").value(0)) // Check that the returned page number is 0
                .andExpect(MockMvcResultMatchers.jsonPath("$['pageable']['paged']").value("true"));
    }

    @Test
    void whenGetRequestToSpeedDifferencesPageFive_thenCorrectResponses() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.get("/api/vessels/3001/speed-differences?page=5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$['number']").value(5)) // Check that the returned page number is 5
                .andExpect(MockMvcResultMatchers.jsonPath("$['pageable']['paged']").value("true"));
    }
}