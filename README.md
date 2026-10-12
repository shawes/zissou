
[![CI](https://github.com/shawes/zissou/actions/workflows/ci.yml/badge.svg)](https://github.com/shawes/zissou/actions/workflows/ci.yml)
[![GitHub issues](https://img.shields.io/github/issues/shawes/zissou.svg)](https://github.com/shawes/zissou/issues)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/shawes/zissou/master/LICENSE.md)
[![DOI](https://zenodo.org/badge/27153222.svg)](https://zenodo.org/badge/latestdoi/27153222)

# ZISSOU

Zissou models connectivity patterns of marine larvae using offline hydrodynamic models in NetCDF format and habitat polygons using a GIS Shapefile. The larval parameters are set using an XML configuration file.

## List of traits that can be specified in the biological model

| Trait                            | Options                              | Description                                                                                                                                                     | Units                                                                      |
|:---------------------------------|:-------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------|:---------------------------------------------------------------------------|
| Pelagic larval duration          | \-                                   | Larvae assigned a PLD sampled from a Gaussian distribution                                                                                                      | Days (Gaussian)                                                            |
| Non-settlement competency period | \-                                   | Larvae are only allowed to settle once competent, competency age sampled from a Gaussian distribution                                                           | Days                                                                       |
| Development age                  | Preflexion                           | Age of preflexion  sampled from a Gaussian distribution. If this value is greater than 0, the larvae are considered to be pelagic eggs during the first period. | Days                                                                       |
|                                  | Flexion                              | Age of flexion sampled from a Gaussian distribution                                                                                                             | Days                                                                       |
|                                  | Postflexion                          | Age of postflexion  sampled from a Gaussian distribution                                                                                                        | Days                                                                       |
| Spawning sites                   | Location                             | The latitude and longitude of larval release location                                                                                                           | GPS Coordinates                                                            |
|                                  | Depth                                | The depth the larvae are spawned                                                                                                                                | metres (m)                                                                 |
|                                  | Number                               | The number of larvae to spawn                                                                                                                                   | Positive integer                                                           |
|                                  | Period                               | The period to release the larvae over                                                                                                                           | Date range                                                                 |
|                                  | Interval                             | The number of days between releases                                                                                                                             | Days (e.g. 1 = daily, 7 = weekly)                                          |
| Settlement sites                 | \-                                   | Polygons representing settlement sites                                                                                                                          | GIS Shapefile                                                              |
| Mortality                        | Linear                               | The percentage of larvae to be randomly killed each day                                                                                                         | Rate (% per day)                                                           |
| Vertical migration               | Diel                                 | Vertically migrates the particles twice daily at sunset and sunrise                                                                                             | Probabilities of larvae day/night distributions at user specified depths   |
|                                  | Ontogenetic                          | Vertically migrates to another depth based on their ontogenetic stage                                                                                           | Probabilities of larvae ontogenetic distributions at user specified depths |
| Settlement buffer                | \-                                   | The distance within which a larva can settle to a reef                                                                                                          | Kilometres (km)                                                            |
| Sensory distance                 | \-                                   | The distance at which a larva can sense a reef and orientate towards it                                                                                         | Kilometres (km)                                                            |
| Horizontal swimming              | Critical swimming speed (*U~crit~*\) | The speed at which a fish can swim before it fatigues as measured in a laboratory setting                                                                       | Metres per second (ms^-1^)                                                 |
|                                  | In situ swimming speed               | The swimming speed of the fish larvae recorded by divers *in situ* as a proportion of the *U~crit~*                                                             | Proportion                                                                 |
|                                  | Swimming endurance                   | The proportion of time the fish can spend swimming as measured in a laboratory setting                                                                          | Proportion                                                                 |


### Reaching land

When the particles reach land they can either be killed, remain in the current position or move with a muted velocity


## Building and running

Zissou needs a JDK (17 or later) and [sbt](https://www.scala-sbt.org/).

```
sbt stage                          # lays the program out in target/universal/stage
scripts/zissou.sh my-config.yaml   # runs one simulation from the command line
scripts/zissou.sh ui               # starts the web interface
```

There is no single "fat" jar: the program is a folder of jars (`target/universal/stage/lib`) that the scripts put on the classpath. Merging everything into one jar broke GeoTools, which finds its plug-ins through `META-INF/services` files that the merge discards, so the shapefiles it wrote held empty geometries.

The launcher uses the throughput (Parallel) garbage collector, which suits a batch job, and leaves the heap at the JVM default (a quarter of the machine's memory). Give a large run more with `ZISSOU_JAVA_OPTS="-Xmx12g" scripts/zissou.sh my-config.yaml`.

### The web interface

`scripts/zissou.sh ui` serves a page at <http://127.0.0.1:8787/> where you can:

- pick a configuration, edit it, and see straight away whether it is valid, what it will release and where, and anything that looks wrong (missing input files, a flow period that would end the run at once, a site outside the supported grid)
- start a run and watch it: the day it has reached, the larvae in the water, and the log as it is written
- cancel a run, and keep a history of runs, each with its own folder holding the exact configuration, the log and the results
- look at what a finished run produced (settlement by release site, the connectivity matrix, how far larvae travelled) and download its files
- see a map of it: the reefs, the release sites, and the paths of the larvae that settled (and, optionally, a sample of all larvae), each coloured by the site it came from. To keep the page quick, a few hundred paths are drawn, thinned and simplified; the heading says how many there really were

Each run is a separate process, so a failure or running out of memory in a simulation cannot take the page down. One run goes at a time.

```
scripts/zissou.sh ui --port 8787 --configs data/synthetic --runs runs
```

Run it from the folder the configurations' relative paths refer to (the repository folder, for the bundled examples). By default it listens on this machine only. `--host 0.0.0.0` makes it reachable from other machines, for example to drive a Mac mini from a laptop, but **there is no login**: anyone who can reach the port can start runs and write configuration files, so only do that on a network you trust. Requests that do not come from the page itself (another website, an unexpected `Host`) are refused.

### Trying it without the original data

Generate a small synthetic dataset (flow files, a reef shapefile and ready-made configurations) and run one of its configurations:

```
sbt "Test/runMain tools.SyntheticDataset"
scripts/zissou.sh data/synthetic/smoke.yaml
```

For a fixed `randomSeed` in the config, runs are reproducible: the output files are identical every time.

## Versioning
I use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/shawes/zissou/tags).

## Authors
- [Steven Hawes](https://github.com/shawes)

## License
[MIT](LICENSE.md) @ Steven Hawes
