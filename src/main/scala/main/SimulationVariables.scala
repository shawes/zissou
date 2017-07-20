package main

import biology.Spawn
import biology.fish.FishParameters
import io.{InputFiles, OutputFiles}
import physical.Turbulence
import physical.flow.Flow
import physical.habitat.Habitat

class SimulationVariables(val fish: FishParameters,
                          val spawn: Spawn,
                          val turbulence: Turbulence,
                          val flow: Flow,
                          val habitat: Habitat,
                          val inputs: InputFiles,
                          val output: OutputFiles) {
  def this() = this(new FishParameters(), new Spawn(), new Turbulence(), new Flow(), new Habitat(), new InputFiles(), new OutputFiles())

  def toXml =
    <SimulationVariables>
      <InputFiles>
        <FlowFilePath>
          {inputs.flowFilePath}
        </FlowFilePath>
        <HabitatFilePath>
          {inputs.habitatFilePath}
        </HabitatFilePath>
      </InputFiles>
      <Spawn>
        <SpawningLocations>
          <SpawningLocation>
          </SpawningLocation>
        </SpawningLocations>
      </Spawn>
    </SimulationVariables>
}
