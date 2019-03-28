# Shuttlecraft
## A small library for deploying your artifacts to the remote repository

Supported remote repositories:
* Nexus snapshots
* Nexus releases (with the staging flow)

Soon to be supported:
* Artifactory
* Local directory

## Usage

See [Demo.scala](https://github.com/odisseus/shuttlecraft/blob/master/demo/Demo.scala)

```scala
implicit val dir = Paths.get(System.getProperty("fury.sharedDir"))

val jar = Paths.get("target", "shuttlecraft.jar")

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

val repo = new NexusRepository(
  uri = "http://localhost:8082/repository/maven-releases/",
  snapshotUri = "http://localhost:8082/repository/maven-snapshots/"
)

repo.publish(artifact, credentials = "admin" -> "admin123")
```

