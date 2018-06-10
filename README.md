# Java / Kotlin MP3Gain Wrapper

JKMP3GainWrapper is an [Mp3Gain](http://mp3gain.sourceforge.net/) wrapper written in [Kotlin](https://kotlinlang.org/), but should be fully compatible to Java.

## Building and Requirements

In order to build a *.jar file JKMP3GainWrapper requires:

* [Gradle](https://gradle.org/) - is used to resolve dependencies and build a jar file.
* [JDK 8+](http://www.oracle.com/technetwork/java/javase/downloads/index.html) - JKMP3GainWrapper is developed with Oracle JDK 8 but should also work with OpenJDK. <br /><br />

To build JKMP3GainWrapper use the following command:

```shell
gradle clean build
```

The \*.jar file can be found in the **build/libs** directory afterwards. Even though, easier is it to directly install it into your maven repository using
```shell
gradle install
```
If you don't want to build it yourself, there is also an already compiled version **_JKMP3GainWrapper-1.0.jar_**

## How to use it

It is just a simple wrapper for MP3Gain. In order to run, it needs a working MP3Gain installation or binary (**mp3gain.exe** for windows or **mp3gain** for e.g. linux based OS'). To instantiate simply use the constructor and provide the path to the MP3Gain binary and optionally change the targetDB and the timestamp option. Per default the target DB is 89. Per default the timestamps of your MP3 file will not be overwritten. Even if MP3Gain writes a new tag, the last modified date of the file itself remains unchanged. 
```java
Mp3Gain g = new Mp3Gain("path/to/MP3Gain/binary")
```

most methods offer an additional parameter called MP3GainThread. This class extends runnable and gets the Error Inputstream of MP3Gain as a parameter and is started by each method itself. Using this thread GUIs for example can show current process.

Afterwards the following methods can be used. It should be mentioned that it only works with up to 15 files, don't ask me why.
```java
Boolean deleteStoredTagInfo(List<String> files)
```
deletes the stored MP3Gain Tags (parameter: _-s d_)

```java
List<UndoMp3GainChange> undoMp3gainChanges(List<String> files, MP3GainThread thread)
```
Undo the changes for the given Files (parameter: _-u_)

```java
List<ApplyGainChange> applyTrackGain(List<String>files, Boolean untilNoClipping, MP3GainThread thread) 
```
applies the track gain so that it will be normalized to default gain. If _untilNoClipping_ is true, the default gain will be ignored and automatically lowered until no clipping occurs. (parameter: _-r -o [-p] [-k] [-d x]_)

```java
List<ApplyGainChange> applyAlbumGain(List<String> files, Boolean untilNoClipping, MP3GainThread thread)
```
applies the album gain so that it will be normalized to default gain (parameter: _-a -o [-p] [-k] [-d x]_)

```java
List<RecommendedGainChange> analyzeGain(List<String> files, MP3GainThread thread) 
```
reads the tag info for the given files. Containing DB and Gain Changes for Track and Album (parameter: _-s r / -s c_)

```java
List<AddGainChange> addGain(List<String> files, Integer gain, MP3GainThread thread) 
```
adds a given gain value to the file. 1 gain = 1.5 DB (parameter: _-g x_)

## Copyright

Copyright (c) 2018 Christian Feier (Christian.Feier@gmail.com). See licence.txt for details.

