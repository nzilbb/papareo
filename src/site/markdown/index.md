# Papa Reo

Java client for for [Papa Reo API](https://papareo.io/docs) 
published by [Te Hiku Media](https://tehiku.nz/te-hiku-tech/)

The API performs speech recognition and alignment for recordings of te reo Māori and 
New Zealand English.

## Usage

You need to `import nzilbb.papareo.PapaReo;` and then instantiate a `PapaReo` object, and set the access token:
```
PapaReo papaReo = new PapaReo().setToken(token);
```

Note that the access token can also be specified by:

- setting the `PAPAREO_TOKEN` environment variable, or
- setting the `papareo.token` system property.

Once that's done, you can invoke the function you need, and check/retrieve the results, e.g.

```
// short utterance transcription:
File wav = new File("short-utterance.wav");
String text = papaReo.transcribeUtterance(new FileInputStream(wav));
System.out.println(text);

// long recording transcription:
File wav = new File("long-speech.wav");

// start transcription task
String taskId = papaReo.transcribeLarge(new FileInputStream(wav));

// wait for it to complete
String status = papaReo.transcribeLargeStatus(taskId);
while (("STARTED".equals(status) || "PENDING".equals(status)) && patience > 0) {
  try {Thread.sleep(1000);} catch(Exception exception) {}
  status = papaReo.transcribeLargeStatus(taskId);
}

// save the resulting VTT file
InputStream stream = papaReo.transcribeLargeDownload(taskId);
File vtt = new File("long-speech.vtt");
java.nio.file.Files.copy(
      stream, 
      vtt.toPath(), 
      StandardCopyOption.REPLACE_EXISTING);
stream.close();
```

## API

Check the
[JavaDoc](apidocs/nzilbb/papareo/PapaReo.html) for full documentation.

