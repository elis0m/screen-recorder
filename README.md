# screen-recorder
_kotlin based recording app_
<br/>
_developed with Android Studio 4.1.3._
 
## Requirements
- Target SDK version : 30
- Min SDK version : 26
 
## Used Android API
- MediaRecorder
- MediaProjection
- VirtualDisplay
- DisplayManager
 
## Preview
### 1. Inital Screen
![image](https://user-images.githubusercontent.com/62126092/119584405-6eeae800-be03-11eb-9e64-48be8d9ecfdd.png)
_At first, there's only a record button on the empty screen._

### 2. Recording Start
![image](https://user-images.githubusercontent.com/62126092/119584427-790ce680-be03-11eb-8997-0168a88f41af.png)
_When the recording starts, you can check the recording time._
<br/>
_You can pause & restart recording if your device has an api level of 24 or higher._

### 3. Recording End & Play recorded video
![image](https://user-images.githubusercontent.com/62126092/119584439-7f9b5e00-be03-11eb-9d70-c4a45395416d.png)
_When the recording is stopped, the recorded video is played in the middle of the screen._

### 4. Saved status
![image](https://user-images.githubusercontent.com/62126092/119105142-fe854500-ba57-11eb-9171-be450481ac71.png)
```kotlin
      output = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
              .toString() + StringBuilder("/")
              .append("Record_")
              .append(SimpleDateFormat("yyyyMMddhhmmss").format(Date()))
              .append(".mp4")
              .toString()
```
_The recorded video will be saved in Movies folder._
<br/>
_If you want to save it in a different path, you can modify the `output`._

