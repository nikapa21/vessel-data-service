package com.deepsea.vesseldataservice.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingServiceImpl implements LoggingService {

    Logger logger = LoggerFactory.getLogger("LoggingServiceImpl");

    @Override
    public void displayReq(HttpServletRequest request, Object body) {

        StringBuilder reqMessage = new StringBuilder();
        Map<String, String> parameters = getParameters(request);

        reqMessage.append("REQUEST:");
        reqMessage.append(" [").append(request.getMethod()).append("]");
        reqMessage.append(" [").append(request.getRequestURI()).append("]");

        if (!parameters.isEmpty()) {
            reqMessage.append(" [").append(parameters).append("]");
        }

        if (!Objects.isNull(body)) {
            reqMessage.append(" [").append(body).append("]");
        }

        logger.info("{}", reqMessage);
    }

    @Override
    public void displayResp(HttpServletRequest request, HttpServletResponse response, Object body) {

        StringBuilder respMessage = new StringBuilder();
        Map<String, String> headers = getHeaders(response);
        respMessage.append("RESPONSE:");
        respMessage.append(" [").append(response.getStatus()).append("]");
        respMessage.append(" [").append(request.getMethod()).append("]");
        if (!headers.isEmpty()) {
            respMessage.append(" [").append(headers).append("]");
        }
        respMessage.append(" [").append(body).append("]");

        logger.info("{}", respMessage);
    }

    private Map<String, String> getHeaders(HttpServletResponse response) {

        Map<String, String> headers = new HashMap<>();
        Collection<String> headerMap = response.getHeaderNames();
        for (String str : headerMap) {
            headers.put(str, response.getHeader(str));
        }
        return headers;
    }

    private Map<String, String> getParameters(HttpServletRequest request) {

        Map<String, String> parameters = new HashMap<>();
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String paramName = params.nextElement();
            String paramValue = request.getParameter(paramName);
            parameters.put(paramName, paramValue);
        }
        return parameters;
    }
}