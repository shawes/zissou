package io.config

import main.SimulationVariables

class ConfigurationAdapter {
  def toVariables(config: Configuration) : SimulationVariables = {
    new SimulationVariables()
  }

  def toConfiguration(variables: SimulationVariables) : Configuration = {
    new Configuration()
  }
}
