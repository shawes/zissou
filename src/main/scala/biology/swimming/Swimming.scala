package biology.swimming

trait Swimming {
  def horizontalSwimming: Option[HorizontalSwimming] = None
  def diel: Option[VerticalMigrationDiel] = None
  def ovm: Option[OntogeneticMigration] = None
}
