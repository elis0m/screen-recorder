# screen-recorder
 ScreenRecorder with Kotlin

## 1. Recording Start
![image](https://user-images.githubusercontent.com/62126092/119105032-e9a8b180-ba57-11eb-94af-3f633105f225.png)
When the recording starts, you can check the recording time. 

## 2. Recording End & Play recorded video
![image](https://user-images.githubusercontent.com/62126092/119105128-fb8a5480-ba57-11eb-8c36-c9678bb68ee7.png)
When the recording is stopped, the recorded video is played in the middle of the screen.

## 3. Saved status
![image](https://user-images.githubusercontent.com/62126092/119105142-fe854500-ba57-11eb-9171-be450481ac71.png)
```kotlin
      output = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
              .toString() + StringBuilder("/")
              .append("Record_")
              .append(SimpleDateFormat("yyyyMMddhhmmss").format(Date()))
              .append(".mp4")
              .toString()
```
The recorded video will be saved in downloads folder.
