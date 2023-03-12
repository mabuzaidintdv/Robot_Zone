# Robot_Zone

This is a project to test Pepper robot human awareness and whether it can detect the presence of humans around it.

## Implementation

The project has 3 main functionalities.
1) Find Humans 
   - Detects humans that are around the robot generally.
   - Return a list of humans that are around the robot.
   
2) Recommend Human To Approach 
   - Detects humans that might be interested in the robot and can be approached.
   - Returns one human only.
   
3) Recommend Human To Engage
   - Detects humans that might be interested in the robot and can be engaged.
   - Returns one human only.

In each of the previous modes, the robot can detect a set of features that are related to the human's status. The robot can detect the following features:
- Age 
- Gender
- Pleasure State
- Excitement State
- Engagement Intention State
- Smile State
- Attention State
- Distance
- Photo
   
After you select the desired Human, you can then order the Robot to either approach or engage with this human.
- Approach: The robot will approach the human and start a conversation.
- Engage: The robot can speak or wave to the human without leaving its place.