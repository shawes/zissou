package biology.swimming

trait Swimming {
    def horizontalSwimming : Option[HorizontalSwimming] = null
    def diel : Option[VerticalMigrationDiel] = null
    def ovm : Option[VerticalMigrationOntogenetic] = null
}



