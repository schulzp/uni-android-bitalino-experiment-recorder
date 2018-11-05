Android Experiment Recorder
===========================

This is an app I created to record experiments for my thesis. It combines

* recording video
* recoding [BITalino](https://bitalino.com/en/) frames
* recording [wahoo](https://de-eu.wahoofitness.com/) heart rate signals
* tagging parts of the experiment

![Screen Shot](https://github.com/schulzp/uni-android-bitalino-experiment-recorder/raw/gh-pages/images/android-app-photo.jpg)

# Third-Party Libraries

## wahoo API

wahoo provides an [Android SDK](http://api.wahoofitness.com/android/api/) which is rather old (2014).
This library (I used [version 1.4.2.5](http://api.wahoofitness.com/android/api/1.4.2.5/)) has to be added to `app/libs`.

## BITalino/PLUX API

Comes embedded, the original source is provided by [BITalinoWorld](https://github.com/BITalinoWorld/revolution-android-api).

# License

Licensed under Apache License 2.0.
