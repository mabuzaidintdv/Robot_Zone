# Robot_Zone

This is a project to test Pepper robot human awareness and whether it can detect the presence of humans around it.

## Implementation

The project is very basic as it doesn't offer a lot control options as offered by Pepper.
1) We implement a SensorListener that starts looking for humans around the robot.
2) If the sensor detects a human, It will start its callback function.
3) There are 3 status for this sensor:
    - **HUMAN_CLOSER:** If the human is moving closer to the robot.
    - **HUMAN_AWAY:** If the human is moving away from the robot.
    - **HUMAN_UNKNOWN:** If the human status is unknown.
   
- Within each status you can implement any needed actions like engaging with this human or saying something to him.