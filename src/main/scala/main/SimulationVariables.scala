package main

import biology.Spawn
import physical.Turbulence
import physical.flow.Flow
import physical.habitat.Habitat
import io.{OutputFiles, InputFiles}
import io.config.FishParameters

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
