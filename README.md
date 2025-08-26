Android application for keyword spotting. 
The keyword detection is based on pre-trained TensorFlow Lite speech recognition models.
To perform inference on a prerecorded audio file, the file must have the following characteristics:
- .wav extension;
- 16 kHz sample rate;
- mono channel.

If these specs are not met, the inference will probably fail.
