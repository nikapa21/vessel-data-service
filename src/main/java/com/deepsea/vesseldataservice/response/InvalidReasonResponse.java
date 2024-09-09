package com.deepsea.vesseldataservice.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InvalidReasonResponse {

    private String reason;
    private Long count;
}