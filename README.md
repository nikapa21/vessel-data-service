# Vessel Data Service

## Overview

The Vessel Data Service is a Spring Boot application designed to manage and process vessel data. It connects to a MySQL database and provides functionalities for reading CSV data, calculating metrics, and storing vessel information.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Setup and Usage](#setup-and-usage)
- [Dependencies](#dependencies)
- [Assumptions](#assumptions)

## Prerequisites

Before you begin, ensure you have met the following requirements:

- **Java 17 or higher**: Make sure you have JDK 17 or higher installed on your machine.
- **Maven**: Ensure you have Maven 3.6.3 or later installed for dependency management and building the project.
- **MySQL**: You need a running MySQL 8.0 instance. The application is configured to connect to a MySQL database.

## Setup and Usage

Follow these steps to set up and run the Vessel Data Service:

1. **Clone the Repository**:
bash
   git clone https://github.com/yourusername/vessel-data-service.git
   cd vessel-data-service


2. **Configure MySQL Database**:
- Ensure that your MySQL server is running. 
- Create a database named `vessel_service_db`:
      
```
CREATE DATABASE vessel_service_db;
```
- Update the `application.properties` file with your MySQL connection settings if necessary:
```
spring.datasource.url=jdbc:mysql://localhost:3307/vessel_service_db?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```
3. **Build the Project**:
- Use Maven to build the project:
```
mvn clean install
```

4. **Run the Application**:
- You can run the application using the following command:
```
mvn spring-boot:run
```

5. **Access the Application**:
- The application will be available at `http://localhost:8080`.

The following endpoints are available in the Vessel Data Service:

### 1. Say Hello
- **Endpoint**: `GET /api/vessels/hello`
- **Description**: Triggers the CSV data processing and insertion.
- **Response**: Returns a message indicating the completion of data processing.

### 2. Get Speed Difference
- **Endpoint**: `GET /api/vessels/{vesselCode}/speed-difference`
- **Description**: Calculates the speed difference for a given vessel.
- **Parameters**:
   - `vesselCode` (Path Variable): The code of the vessel.
   - `latitude` (Query Parameter, optional): The latitude for the calculation (must be a valid number).
   - `longitude` (Query Parameter, optional): The longitude for the calculation (must be a valid number).
- **Response**: Returns a list of `SpeedDifferenceResponse` objects.

### 3. Get Invalid Reasons
- **Endpoint**: `GET /api/vessels/{vesselCode}/invalid-reasons`
- **Description**: Retrieves the invalid reasons for a given vessel.
- **Parameters**:
   - `vesselCode` (Path Variable): The code of the vessel.
- **Response**: Returns a list of `InvalidReasonResponse` objects.

### 4. Compare Vessel Compliance
- **Endpoint**: `GET /api/vessels/compare-compliance`
- **Description**: Compares the compliance of two vessels.
- **Parameters**:
   - `vesselCode1` (Query Parameter): The code of the first vessel.
   - `vesselCode2` (Query Parameter): The code of the second vessel.
- **Response**: Returns a message indicating which vessel is more compliant or if they are equal.

### 5. Get Vessel Data for Period
- **Endpoint**: `GET /api/vessels/{vesselCode}/data`
- **Description**: Retrieves vessel data for a specified period.
- **Parameters**:
   - `vesselCode` (Path Variable): The code of the vessel.
   - `startDate` (Query Parameter): The start date for the data retrieval (format: YYYY-MM-DD).
   - `endDate` (Query Parameter): The end date for the data retrieval (format: YYYY-MM-DD).
- **Response**: Returns a list of `ValidVesselData` objects for the specified period.

## Dependencies

The project uses the following dependencies:

- **Spring Boot Starter**: For building Spring applications.
- **Spring Data JPA**: For data access and repository support.
- **MySQL Connector**: For connecting to the MySQL database.
- **Hibernate**: For ORM (Object-Relational Mapping).
- **JUnit**: For unit testing.
- **Mockito**: For mocking in unit tests.

## Assumptions

//TODO consecutive waypoints what I mean: 1 hour continuous errors considered to be part
of the same problem. timestamp instead of geolocation (easier)
in the cluster, in the groups, microgroups of < 10 are ignored and
work with bigger groups. these are default values and can be overriden
by the controller

Utilizing Python Interquartile Range (IQR) Method, offline calculations took place to estimate outlier values of the dataset (see plot below).

![img.png](src/main/resources/img.png)

Also, mysql queries, like following
```
SELECT COUNT(*) AS count_above_20
FROM valid_vessel_data
WHERE CAST(actual_speed_overground AS DECIMAL) > 20;
```
- **actual_speed_overground > 20**

Applying same logic the rest of the outlier values are:

- **proposed_speed_overground > 20**


