
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
| Mortality                        | Linear                               | The share of larvae to be randomly killed each day, needed when `isMortal` is true                                                                              | Share per day, from 0 to 1 (0.26 kills 26% a day)                          |
| Vertical migration               | Diel                                 | Vertically migrates the particles twice daily at sunset and sunrise                                                                                             | Probabilities of larvae day/night distributions at user specified depths   |
|                                  | Ontogenetic                          | Vertically migrates to another depth based on their ontogenetic stage                                                                                           | Probabilities of larvae ontogenetic distributions at user specified depths |
| Settlement buffer                | \-                                   | The distance within which a larva can settle to a reef                                                                                                          | Kilometres (km)                                                            |
| Sensory distance                 | \-                                   | The distance at which a larva can sense a reef and orientate towards it                                                                                         | Kilometres (km)                                                            |
| Horizontal swimming              | Critical swimming speed (*U~crit~*\) | The speed at which a fish can swim before it fatigues as measured in a laboratory setting                                                                       | Metres per second (ms^-1^)                                                 |
|                                  | In situ swimming speed               | The swimming speed of the fish larvae recorded by divers *in situ* as a proportion of the *U~crit~*                                                             | Proportion                                                                 |
|                                  | Swimming endurance                   | The proportion of time the fish can spend swimming as measured in a laboratory setting                                                                          | Proportion                                                                 |


### Reaching land

When the particles reach land they can either be killed, remain in the current position or move with a muted velocity


### Turbulence

Turbulent mixing is a random walk, set by `turbulence.horizontalDiffusionCoefficient` and `turbulence.verticalDiffusionCoefficient` in m2/s. Every time step moves each larva a normally distributed distance east-west, north-south and vertically, drawn independently, with variance 2·K·Δt along each axis, so a patch of larvae spreads with variance 2·K·t however long the step is. The vertical part only acts when `flow.includeVerticalVelocity` is true.

Earlier versions of Zissou drew a uniform number instead, which carries a third of the variance, and added the same number east-west and north-south, so larvae were only ever pushed along a diagonal. A coefficient chosen for those runs spreads larvae about three times as far now; dividing it by 3 reproduces the old mean squared spread, though not the old direction.

### Flow and time

The flow is read as daily fields, from monthly files laid out as `<flow folder>/{u,v,w}/<name>_<variable>_<year>_<month>.nc`. Each field is for the time stamped on it in the file (noon, for daily means), and the flow a larva feels at any moment is a straight-line blend of the two fields either side of that moment, in proportion to how near the moment is to each, so it changes smoothly through the day and does not jump at midnight. Until the time of the first field the first field is held. The simulation clock is compared with the time axis of the files as UTC.

Across the grid, the flow at a position is interpolated from the nodes round it: bicubic where the 4 by 4 nodes are all sea, bilinear where only the 2 by 2 are, and the nearest node's flow where there is land or the edge of the grid. The nodes are chosen from the coordinates in the files, so the latitude axis may run either way and the grid need not be 0.1 degrees. Earlier versions chose the wrong nodes and returned the flow of a neighbouring cell, up to a whole grid cell away.

Earlier versions of Zissou took the flow from one to two days ahead of the clock and used one day's field for the whole day: the interpolation between today's and tomorrow's field read today's field twice.

The files must follow on month after month, one for each month from the one the run starts in, with no gap; Zissou stops with a message if one is missing. If the flow files end before the run does, the simulation stops there with a warning in its log and the results of what it simulated are still written. Earlier versions never read the last file in the folder, and when the months ran out silently started the last month's flow again.

### Moving the larvae

Each time step a larva is moved by the classical fourth order Runge-Kutta method. The flow is sampled four times: where the larva is at the start of the step (k1); at the middle of the step, from where k1 would have carried it by then (k2); at the middle again, from where k2 would have (k3); and at the end, from where k3 would have (k4). The larva then moves at the weighted mean (k1 + 2·k2 + 2·k3 + k4) / 6, with turbulence added to the mean and its swimming velocity added to that. Each sample is the flow at its own time as well as its own place. If the step would take a larva into land, the sea floor or the coast, it goes 10 m higher than that instead if there is sea there, and otherwise stays where it was.

Earlier versions sampled the flow a whole step too far along each leg (at 1, 1.5, 1.5 and 2 steps' worth of travel from the start, where the method has 0, 0.5, 0.5 and 1), took the starting flow from the nearest cell and left it out of the mean. That made a larva carried round an eddy spiral in towards its centre: one that the exact flow takes round a circle of radius 50 km was 36 km from the centre after five days of 2-hour steps, and where it ended depended on the length of the step. Results that depended on larvae dispersing through eddies will move.

### Swimming

`larva.swimming.ability` says how a larva that can swim chooses where to go:

- `directed`: towards the nearest reef it can sense (within the olfactory distance) once it is postflexion, and in a random direction otherwise;
- `undirected`: in a random direction, a new one each step;
- `passive` (or left out): it does not swim at all, whatever its swimming speed.

Earlier versions let a passive larva swim like an undirected one.

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
