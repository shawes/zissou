
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

Land, in an ocean model's data, is where there is no flow, and the coast of a model with cells of 10 km is a cell or two out from the real one. When a time step would take a larva onto land, below the sea floor or onto the coast:

- if it is below the sea floor, it goes 10 m higher than that instead, if there is sea there;
- otherwise it goes as far as it can in the direction it was going, that is in the sea: three quarters, a half or a quarter of the way, or the whole way along one of the two axes (east-west or north-south) on its own. A larva carried into a coast and along it goes on along the coast, and one carried straight at it goes up to it;
- and where it can go nowhere, as when it is carried into a corner, it stays where it was for that step.

A larva that is on land at the start of a step has no flow to move it and stays. A flow sample of the four of a step that is over land is given the flow of the sample before it, so that one sample off the coast does not cancel the step. A larva that drifts off the edge of the flow grid is killed, and the log says how many were.

Earlier versions cancelled a step altogether when it ended on land, or when any of its four samples was over land, so a larva carried against a coast stopped there for good: on the NSW coast, in BRAN2020, a quarter of the larvae that settled had never moved, and the paths hugged the shore. Earlier versions of this README also said a larva reaching land could be killed or moved at a reduced speed; there has been no setting for either.


### Turbulence

Turbulent mixing is a random walk, set by `turbulence.horizontalDiffusionCoefficient` and `turbulence.verticalDiffusionCoefficient` in m2/s. Every time step moves each larva a normally distributed distance east-west, north-south and vertically, drawn independently, with variance 2·K·Δt along each axis, so a patch of larvae spreads with variance 2·K·t however long the step is. The vertical part only acts when `flow.includeVerticalVelocity` is true.

Earlier versions of Zissou drew a uniform number instead, which carries a third of the variance, and added the same number east-west and north-south, so larvae were only ever pushed along a diagonal. A coefficient chosen for those runs spreads larvae about three times as far now; dividing it by 3 reproduces the old mean squared spread, though not the old direction.

### Flow and time

The flow is read as daily fields, from monthly files laid out as `<flow folder>/{u,v,w}/<name>_<variable>_<year>_<month>.nc`. Each field is for the time stamped on it in the file (noon, for daily means), and the flow a larva feels at any moment is a straight-line blend of the two fields either side of that moment, in proportion to how near the moment is to each, so it changes smoothly through the day and does not jump at midnight. Until the time of the first field the first field is held. The simulation clock is compared with the time axis of the files as UTC.

Only part of each field is read: the `flow.bounds` (`south`, `north`, `west`, `east`, in degrees, longitude as the files give it) and the first `flow.depthLevels` levels from the surface. Without them Zissou reads latitudes 50 to 10 south, longitudes 142 to 180 east and 15 levels, as it always has. Larvae that leave the region are killed like those that leave the grid, so give it room round the area you are studying. The depth of each level is read from the files' own depth axis; it fails with a message if they have fewer levels than `depthLevels` asks for. Earlier versions fixed the region and the depths in the program.

Across the grid, the flow at a position is interpolated from the nodes round it: bicubic where the 4 by 4 nodes are all sea, bilinear where only the 2 by 2 are, and the nearest node's flow where there is land or the edge of the grid. The nodes are chosen from the coordinates in the files, so the latitude axis may run either way and the grid need not be 0.1 degrees. Earlier versions chose the wrong nodes and returned the flow of a neighbouring cell, up to a whole grid cell away.

Ocean models usually put the three velocity components on different grids. In BRAN2020, for example, `u` and `v` are on the corners of the cells (`xu_ocean`, `yu_ocean`) and the centres of the layers (`st_ocean`), and `w` is on the centres of the cells (`xt_ocean`, `yt_ocean`) and the bottoms of the layers (`sw_ocean`): half a cell, about 5 km, and half a level from the others. Where the files are laid out like that, Zissou reads and interpolates each component on its own grid, at the nearest level of its own depths, and there is flow only where all three have it. Where they share a grid, as in the test data, nothing changes. Earlier versions took the nodes and the levels of `u` for all three, so `w` was read from the wrong place by half a cell and half a level.

Earlier versions of Zissou took the flow from one to two days ahead of the clock and used one day's field for the whole day: the interpolation between today's and tomorrow's field read today's field twice.

The files must follow on month after month, one for each month from the one the run starts in, with no gap; Zissou stops with a message if one is missing. If the flow files end before the run does, the simulation stops there with a warning in its log and the results of what it simulated are still written. Earlier versions never read the last file in the folder, and when the months ran out silently started the last month's flow again.

#### Reading the flow from an OPeNDAP server

Instead of a folder, `flow.netcdfFilePath` can be the address of a directory of monthly files on an OPeNDAP server, such as a THREDDS `dodsC` directory, so that nothing has to be downloaded first. For CSIRO's BRAN2020 daily flow at NCI:

```yaml
flow:
  netcdfFilePath: https://thredds.nci.org.au/thredds/dodsC/gb6/BRAN/BRAN2020/daily
  bounds: {south: -33, north: -24, west: 150, east: 158}   # BRAN2020 is global: read only what you need
  period:
    start: 2019-01-25
    end: 2019-02-10
```

The files are `<address>/<prefix>_<variable>_<year>_<month>.nc`, all in the one directory (`ocean_u_2019_01.nc`, `ocean_v_2019_01.nc`, `ocean_w_2019_01.nc`); `flow.filePrefix` changes `ocean` for a model that names them differently. Only the part of each day that is read, the `bounds` and `depthLevels`, crosses the network, about 4 seconds a day for BRAN2020 from Australia. While the larvae move, the day after the next is read on a thread of its own, so the run waits only when a time step takes less time than that. A request that fails is tried again a few times before the run stops with the reason.

A server cannot list its files, so the data end at the first month it has no file for (December 2023 for BRAN2020, which begins in January 1993): a run that reaches it stops there with a warning, as with a folder, and a gap in the months cannot be told from the end. Nothing is checked about an address until the run starts, so a mistyped one fails there, saying which month it found no file for. `ZISSOU_LIVE_TESTS=1 sbt "testOnly io.Bran2020LiveTest"` runs the tests that read BRAN2020 itself.


#### Finer models nested inside the flow

A model with cells of about 10 km, such as BRAN2020, does not resolve the coast, so near a reef its currents and its coastline are poor. If you have a finer model of part of the coast (a regional ROMS, say), list it under `flow.nested` and larvae inside it are carried by it instead:

```yaml
flow:
  netcdfFilePath: https://thredds.nci.org.au/thredds/dodsC/gb6/BRAN/BRAN2020/daily
  bounds: {south: -33, north: -24, west: 150, east: 158}
  nested:                                   # finest first
    - netcdfFilePath: /data/roms-moreton-bay
      bounds: {south: -28.2, north: -27.0, west: 152.8, east: 153.8}
      depthLevels: 30                       # optional: the main flow's if left out
      filePrefix: roms                      # optional: "ocean" if left out
```

Each nested model is laid out like the main one (monthly `u`, `v` and `w` files, in a folder or on an OPeNDAP server) and is read for the same period and time step; it has its own grid, depth levels and file names, and only the part inside its `bounds` is read. Any position on a nested model's grid takes its flow, interpolated on that grid; the nested models are tried in the order listed, so list the finest first, and a position on none of them takes the main model's. Inside a nested model the coast is the nested model's: where it has no flow (land), a larva is on land, even where the main model has sea. The flow is not blended across the edge of a nested model, so a larva crossing it meets a change in flow there.

##### Using ROMS output as a nested model

`scripts/roms_to_zissou.py` converts ROMS history or average files (such as the UNSW EAC model) to the monthly daily-mean files a nested model needs. It needs `pip install numpy netCDF4 scipy`.

```
scripts/roms_to_zissou.py --out /data/roms-eac --resolution 0.02 --prefix roms roms_his_*.nc
```

For each record it moves `u` and `v` from the staggered C-grid to the cell centres, rotates them to east and north with the grid's `angle`, interpolates `u`, `v` and `w` from the terrain-following levels to fixed depths (BRAN2020's top 15 unless `--depths` says otherwise; `Vtransform` 1 and 2), and interpolates to a regular longitude/latitude grid of `--resolution` degrees. A node is land where the nearest ROMS cell is land, so the coast stays ROMS's own; below the sea floor there is no flow. It averages the records of each UTC day and writes `<out>/{u,v,w}/<prefix>_<variable>_<year>_<month>.nc`. At the end it prints the `flow.nested` entry to use, with the bounds and the number of levels. `--south/--north/--west/--east` write only part of the ROMS grid, `--grid` names a file to take the grid from when the output files do not carry it, and `--vtransform` supplies it when the files do not say.

A month the ROMS files do not cover in full is not written, because Zissou reads a whole month at a time, and the script says which. If the files have no `w`, it writes 0 and tells you to set `flow.includeVerticalVelocity` to false. ROMS's `w` is taken to be the vertical velocity in m/s. The script is tested on a made-up rotated ROMS grid (`python3 scripts/test_roms_to_zissou.py`) and its output read by Zissou, but not yet on a real model's files; the variable names are ROMS's standard ones.

A nested model must have files for the month the run starts in or the run does not start. If its files end before the run does, the log warns of it and the main flow carries larvae through the area from then on. Larvae are only killed for leaving the main flow's grid.

### Moving the larvae

Each time step a larva is moved by the classical fourth order Runge-Kutta method. The flow is sampled four times: where the larva is at the start of the step (k1); at the middle of the step, from where k1 would have carried it by then (k2); at the middle again, from where k2 would have (k3); and at the end, from where k3 would have (k4). The larva then moves at the weighted mean (k1 + 2·k2 + 2·k3 + k4) / 6, with turbulence added to the mean and its swimming velocity added to that. Each sample is the flow at its own time as well as its own place. If the step would take a larva into land, the sea floor or the coast, it is taken as far as it can go in the sea, along the coast if that is how the flow runs (see "Reaching land").

Earlier versions sampled the flow a whole step too far along each leg (at 1, 1.5, 1.5 and 2 steps' worth of travel from the start, where the method has 0, 0.5, 0.5 and 1), took the starting flow from the nearest cell and left it out of the mean. That made a larva carried round an eddy spiral in towards its centre: one that the exact flow takes round a circle of radius 50 km was 36 km from the centre after five days of 2-hour steps, and where it ended depended on the length of the step. Results that depended on larvae dispersing through eddies will move.

A step can run on past the time of the later of the two daily fields the clock lies between, when the step does not divide 12 hours (or the fields are not for noon). The samples after that time are blended from the next pair of fields. Earlier versions held the later field for the rest of the step instead. Where the flow files end within a step, the last field is still held for the rest of it.

### Swimming

`larva.swimming.ability` says how a larva that can swim chooses where to go:

- `directed`: towards the nearest reef it can sense (within the olfactory distance) once it is postflexion, and in a random direction otherwise;
- `undirected`: in a random direction, a new one each step;
- `passive` (or left out): it does not swim at all, whatever its swimming speed.

Earlier versions let a passive larva swim like an undirected one.

### Days, depths and settling

The simulation clock is UTC, as the flow files are. Larvae are released, and daily mortality and the daily ontogenetic migration (`ovmProbabilities.implementation: daily`) are applied, on the first time step of each day, whatever the length of the step.

Diel migration follows the sun where the larva is. At dawn the larva goes to a depth drawn from the `day` probabilities, and at dusk to one drawn from the `night` probabilities. It migrates on the first step within 45 minutes of sunrise or sunset, or within a time step when the step is longer than that. A larva with ontogenetic migration as well takes its depth from that, and diel migration takes it to a day depth from dawn to dusk. A change of stage by day changes the depth it goes back to at dusk. A `day` or `night` whose probabilities are all 0 leaves the larva where it is at dawn or at dusk; with ontogenetic migration as well, a `day` of all 0 keeps the larva at its ontogenetic depth all day, and a change of stage by day moves it at once.

With ontogenetic migration by stage (`implementation: stage`), a larva goes to a depth drawn from its stage's probabilities on its first step, for the stage it is born in, and on each change of stage after that. A stage whose probabilities are all 0 leaves the larva at the depth it has. Earlier versions migrated only on a change of stage, so the `hatching` probabilities were never used, and a larva born in preflexion stayed at its spawning depth until flexion.

Each set of migration probabilities (diel `day` and `night`, and each stage of `ovmProbabilities`) needs one value for each depth band, none below 0, and must add up to 1 or be all 0. Otherwise the run stops before it starts with a message saying what to change, and the web interface rejects the configuration with the same message. Earlier versions ran with probabilities that did not add up: a set adding up to less than 1 gave what was left over to the deepest band, one adding up to more never reached its last bands, and a set of all 0 sent every larva to the deepest band.

A larva is in its first stage (the one the `hatching` migration probabilities are for) from birth until preflexion. `ontogeny.hatching` can be left out: it is read but not used. Earlier versions drew a hatching age for each larva and did nothing with it.

A larva can settle once it is older than its competency period (`nonSettlementPeriod`, which is capped at its PLD), and dies of old age once it is older than its PLD, without settling on that step.

A run can end with larvae still drifting, when its period, or the flow files, end before their PLDs do. They are written with the rest: their paths, up to where they are at the end, are in the dispersal shapefile and the larvae history (with the state `pelagic`), and the log says how many there were. They are in neither the connectivity matrix nor the dispersal kernel, which are for larvae that settled. Earlier versions left them out of the results altogether.

Earlier versions:

- read the clock as local time when finding sunrise and sunset, and used the `day` probabilities by night and the `night` ones by day. Off eastern Australia the two nearly cancelled, leaving larvae at their day depths from about 05:00 to 15:00 local; elsewhere they were further out;
- did the daily things on every step that started between 00:00 and 01:00: twice a day with half hour steps, and only on some days with steps that do not divide a day;
- left a larva with both migrations at its day depth for good once dawn lasted more than one step;
- let a larva that died of old age settle on the step it died.

## Building and running

Zissou needs a JDK (17 or later) and [sbt](https://www.scala-sbt.org/).

```
sbt stage                          # lays the program out in target/universal/stage
scripts/zissou.sh my-config.yaml   # runs one simulation from the command line
scripts/zissou.sh --parallel 2 a.yaml b.yaml c.yaml   # several, two at a time
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
- see a map of it: the land, the reefs larvae were released from (not the thousands of others in a habitat layer), the release sites, and the paths of the larvae that settled (and, optionally, a sample of all larvae), each coloured by the site it came from. To keep the page quick, a few hundred paths are drawn, thinned and simplified; the heading says how many there really were

Each run is a separate process, so a failure or running out of memory in a simulation cannot take the page down. One run goes at a time unless you start it with `ui --parallel N`, which allows N at once; each then gets its share of the cores. The same goes for the command line: with several configurations, `--parallel N` (default: all of them) runs that many at once, each in its own process, printing its lines prefixed with the configuration's name. Give runs that go together their own `saveOutputFilePath`. Mortality and movement already use every core within a run, so extra runs help most when a run is small or spends time reading flow files.

```
scripts/zissou.sh ui --port 8787 --configs data/synthetic --runs runs --land data/land/ne_10m_land.shp
```

The land on the map is drawn from a shapefile of the world's land, so that what is not land shows as water. It is not part of Zissou: get [Natural Earth's 10 m land](https://www.naturalearthdata.com/downloads/10m-physical-vectors/10m-land/) (public domain, 3 MB) and unpack it where Zissou looks for it, or give another coastline with `--land`:

```
mkdir -p data/land && curl -L -o data/land/ne_10m_land.zip https://naturalearth.s3.amazonaws.com/10m_physical/ne_10m_land.zip && unzip -o data/land/ne_10m_land.zip -d data/land
```

Without it maps are drawn with no land. Only the part of the coast in view is sent to the page, simplified to about a pixel.

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
