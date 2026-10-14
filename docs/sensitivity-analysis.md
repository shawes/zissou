# Sensitivity analysis plan

How much do Zissou's dispersal results depend on the larval traits, the settlement rules, the numerics and the ocean
year? So far each factor has been changed on its own, once, with one random seed, in one spawning season
(November 2019 to March 2020). This plan sets out a fuller analysis: how big the random noise is, which factors matter,
whether they interact, and how the answers change from year to year.

## Questions

1. How much do the results vary with the random seed alone? Every other effect is judged against that.
2. Which larval traits and model settings change the results most, and which hardly matter?
3. Do factors interact: does the effect of one depend on the value of another?
4. How much do the results change from one spawning season to the next, compared with the traits?
5. Is the ranking of factors the same in every year, or does it depend on the ocean?

## Baseline

The 12 day NSW experiment (`data/experiments/configs/nsw-vic-12d-5pc.yaml`, merged habitat `data/seamap/nsw_vic_reef.shp`):
six release sites from the Solitary Islands to Eden, 900 larvae per site every 7 days, BRAN2020 daily flow,
2 hour steps, 12 day competency, 5% daily mortality, 5 km settlement and 10 km smell distances.

## Outputs

Every run is summarised in one CSV row, so that runs can be compared directly.

| Output | What it measures |
|---|---|
| Settlement success | Share of the larvae released that settle |
| Median, 90th percentile and maximum distance | Straight-line distance from release to settlement |
| Self-recruitment | Share of settlers that settle on their own release reef |
| Local retention | Share of settlers within 10 km of their release site |
| Connectivity difference | Bray–Curtis distance between the run's connectivity matrix and the baseline's |
| Links kept | Share of the baseline's source-to-sink links that the run also has |
| Per release site | Settled count and median distance for each site |
| Export to Victoria | Share of settlers on Victorian reefs |
| Northward transport | Share of larvae that end north of their release latitude |

## Factors

The ranges are placeholders until a target species is chosen; they should then come from the literature for it.

| Group | Factor | Configuration field | Range |
|---|---|---|---|
| Biology | Competency (non-settlement period) | `larva.pelagicLarvalDuration.nonSettlementPeriod` | 5–15 days |
| | Pelagic larval duration | `larva.pelagicLarvalDuration.mean` | 15–40 days |
| | Swimming | `criticalSwimmingSpeed`, `inSituSwimmingPotential`, `endurance` | passive to strong |
| | Vertical migration | `larva.ovmProbabilities`, `larva.dielProbabilities` | none, shallow, deep |
| | Mortality | `larva.mortalityRate`, `temperatureMortality.q10` | 0–20% a day; Q10 1–3 |
| | Development | `larva.ontogeny.flexion`, `postflexion` | ±50% |
| Settlement | Settlement and smell distances | `habitat.buffer.settlement`, `olfactory` | 1–10 km; 5–20 km |
| Physics | Diffusion | `turbulence.horizontalDiffusionCoefficient`, `verticalDiffusionCoefficient` | 0–100 m²/s |
| | Release depth | `spawn.spawningLocation[].site.depth` | 0–50 m |
| Numerics | Time step, turbulence interval | `flow.timeStep`, `turbulence.interval` | 1, 2, 3 hours |
| | Larvae per release | `numberOfLarvae` | 300–2,700 per site |
| Environment | Spawning season | `flow.period`, `releasePeriod` | 5 or more seasons (below) |
| | Timing within the season | `releasePeriod`, `interval` | early, middle, late |

## Comparing years

Years should be compared. The East Australian Current's southward extension, where it leaves the coast and the eddies
south of it change a great deal from year to year, and those are what carry the larvae. All the results so far are from
one season, so they may describe 2019–20 rather than the system. Two questions need several years:

- How much the results change from year to year, against how much the traits change them. In many dispersal studies the
  year matters as much as, or more than, the larval traits.
- Whether the factors that matter in 2019–20 matter in other years too. A trait that matters only in some years is a
  finding in itself.

### Seasons

Five spawning seasons (November to March), chosen to cover contrasting ocean conditions. The choice is to be checked
against the BRAN2020 record and ENSO and EAC indices before any runs:

| Season | Why |
|---|---|
| 2019–20 | The baseline so far |
| 2010–11 | Strong La Niña |
| 2015–16 | Strong El Niño |
| 2021–22 | La Niña |
| 1997–98 | Strong El Niño, early in the record |

### Design

- **Stage 5** runs the baseline in every season, with 3 seeds each, to measure year-to-year spread against seed noise.
- **Stage 6** repeats the one-at-a-time runs for the 4–5 factors that mattered most in stage 3, in the two most
  contrasting seasons, to test whether the ranking of factors holds.

## Stages

### 0. Setup

- **Batch tool:** A tool that expands a design file (a baseline plus factor values) into configurations, runs them in
  parallel and collects the outputs above into one CSV. It replaces the ad hoc sweep script.
- **Local flow cache:** A 12 day run takes about 8 minutes, and most of that is waiting for BRAN2020 over OPeNDAP.
  Reading a local copy of the NSW subset would make a few hundred runs practical. The size of each season's subset is
  to be checked before anything is downloaded.
- **Metrics:** Add the outputs above that the results reader does not give yet (Bray–Curtis distance, links kept,
  local retention, northward transport).

### 1. Noise and convergence

- Baseline with 5 seeds: the spread is the noise floor.
- Baseline with 300, 900 and 2,700 larvae per site: the number above which the outputs stop changing.

### 2. Numerical checks

- Time step of 1, 2 and 3 hours, and turbulence intervals to match. The results should not depend on them; if they do,
  that is fixed before going on.

### 3. One factor at a time

- Each factor at its low and high value around the baseline: about 30 runs.
- Ranks the factors by their effect on each output, against the noise floor.

### 4. Global screening (Morris)

- The 8–10 factors that mattered in stage 3, with 10 trajectories: about 100 runs.
- The mean of each factor's elementary effects shows its importance; their spread shows interactions and
  non-linearity.

### 5. Years

- The baseline in each of the five seasons, with 3 seeds: 15 runs.

### 6. Factors across years

- Stage 3 for the top 4–5 factors in the two most contrasting seasons: about 20 runs.

### 7. Variance decomposition (Sobol), if needed

- Only if stage 4 shows interactions strong enough to be worth separating. It needs thousands of runs.

## Cost

Stages 1 to 6 come to about 200–250 runs. At 3–5 minutes a run with local flow data, and several at once, that is one
to two days of machine time. Over OPeNDAP it is several times longer.

## Open questions

- **Species:** which species or group the ranges should come from.
- **Main question:** connectivity, retention or export to Victoria; it decides which output leads.
- **Release sites:** whether to add a site north of the Solitary Islands, which matters only if northward transport
  turns out to be more than marginal in some years.
