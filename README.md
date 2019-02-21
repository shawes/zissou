
[![Build](https://img.shields.io/travis/shawes/zissou.svg)](https://travis-ci.org/shawes/zissou)
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


## Versioning
I use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/shawes/zissou/tags).

## Authors
- [Steven Hawes](https://github.com/shawes)

## License
[MIT](LICENSE.md) @ Steven Hawes
