# Flight Schedule and Weather Forecast Checker

## Overview

This Java program checks the status of flights based on the schedule and weather forecast. It analyzes information about flights between cities and the current weather forecast in these cities to determine whether each flight is scheduled on time or canceled.

## Data Structure

### Flights (`flights`):

- **`no`**: Flight number.
- **`departure`**: Departure time in LOCAL time.
- **`from`**: Departure city.
- **`to`**: Destination city.
- **`duration`**: Flight duration in hours.

### Weather Forecast (`forecast`):

- **`time`**: LOCAL time.
- **`wind`**: Wind speed in m/s.
- **`visibility`**: Visibility in meters.

## Flight Execution Criteria

A flight is considered to be scheduled on time if flying weather is observed at the time of departure and arrival in the respective cities, defined by the following conditions:

- Wind speed does not exceed 30 m/s.
- Visibility is at least 200 meters.

All other flights are considered canceled.

## Output Format

The program outputs the status of each flight in the following format:


### Examples

- `F1 | Moscow -> Novosibirsk | On Schedule`
- `F2 | Moscow -> Omsk | Canceled`
