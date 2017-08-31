package biology

import org.apache.commons.math3.distribution.NormalDistribution
import locals.DistributionType.DistributionType

class PelagicLarvalDuration(val distribution: NormalDistribution, val distributionType: DistributionType){}
