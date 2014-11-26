package io.config

import main.SimulationVariables

class ConfigurationAdapter {
  def ToVariables(config: Configuration) = {
    new SimulationVariables()
  }

  def ToConfiguration(variables: SimulationVariables) = {

  }
}
