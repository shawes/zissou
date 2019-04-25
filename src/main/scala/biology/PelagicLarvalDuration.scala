package biology

import org.apache.commons.math3.distribution.NormalDistribution
import locals.DistributionType.DistributionType
import locals.PelagicLarvalDurationType

class PelagicLarvalDuration(
    val distribution: NormalDistribution,
    //val distributionType: DistributionType,
    val isFixed: Boolean,
    val nonSettlementPeriod: Double
) {}
