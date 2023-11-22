# Papa Reo

Java client for the [Papa Reo API](https://papareo.io/docs) 
published by [Te Hiku Media](https://tehiku.nz/te-hiku-tech/)

The API performs speech recognition and alignment for recordings of te reo Māori and 
New Zealand English.

## Usage

Full documententation is available here: <https://nzilbb.github.io/papareo/>

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

## Developers

Running the automated tests requires a valid Papa Reo API token.

Once you have a valid tokem, create a file called `papareo.properties` in your home
directory, with contents like: 
```
papareo.token=xxxxxx-xxxx-xxxx-xxxx-xxxxxxx
```

Then the automated tests will run.

To build the package without running automated tests:

```
mvn package -Dmaven.test.skip
```

## Deploying to OSSRH

OSSRH is the central Maven repository where nzilbb.ag modules are deployed (published).

There are two type of deployment:

- *snapshot*: a transient deployment that can be updated during development/testing
- *release*: an official published version that cannot be changed once it's deployed

A *snapshot* deployment is done when the module version (`version` tag in pom.xml) ends with
`-SNAPSHOT`. Otherwise, any deployment is a *release*.

### Snapshot Deployment

To perform a snapshot deployment:

1. Ensure the `version` in pom.xml *is* suffixed with `-SNAPSHOT`
2. Execute the command:  
   ```
   mvn clean deploy
   ```

### Release Deployment

To perform a release deployment:

1. Ensure the `version` in pom.xml *isn't* suffixed with `-SNAPSHOT` e.g. use something
   like the following command from within the ag directory:  
   ```
   mvn versions:set -DnewVersion=1.1.0
   ```
2. Execute the command:  
   ```
   mvn clean deploy -P release
   ```
3. Happy with everything? Complete the release with:
   ```
   mvn nexus-staging:release -P release
   ```
   Otherwise:
   ```
   mvn nexus-staging:drop -P release
   ```
   ...and start again.
4. Regenerate the citation file:
   ```
   mvn cff:create
   ```
5. Commit/push all changes and create a release in GitHub
