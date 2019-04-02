# Shuttlecraft
## A small library for deploying your artifacts to the remote repository

Supported remote repositories:
* Maven2 (including Nexus and Artifactory)

Soon to be supported:
* Nexus release staging
* Publishing to a local directory (e. g. `~/.m2`)

## Usage

See [Demo.scala](https://github.com/odisseus/shuttlecraft/blob/master/demo/Demo.scala)

```scala
implicit val dir = Paths.get(System.getProperty("fury.sharedDir"))

val jar = Paths.get("target", "shuttlecraft-demo.jar")

val artifact = Artifact(
  groupId = "com.example",
  artifactId = "shuttlecraft",
  version = "0.0.1-SNAPSHOT",
  author = None,
  license = None,
  dependencies = Set(
    ("com.lihaoyi","ujson_2.12","0.7.1")
  ),
  jar = jar
)

val mvnApi = new Maven2HttpApi(
  repositoryUrl = "http://localhost:8081/nexus/content/repositories/snapshots/",
  username = "admin",
  password = "admin123"
)

val bar = new Maven2ResourceFactory(gpgPassphrase = None, signed = false)

val publisher = new Maven2Publisher(mvnApi, resourceGen)

publisher.publish(artifact).recover{ case NonFatal(e) => e.printStackTrace }
```

