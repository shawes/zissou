package main

import org.scalatest.{FlatSpec, PrivateMethodTester}

class LarvaeDisperserTest extends FlatSpec with PrivateMethodTester {


  /*  val context = JAXBContext.newInstance(classOf[Configuration])
    val configXml = new File("src/test/scala/data/test_config2.xml")
    val config = context.createUnmarshaller().unmarshal(configXml).asInstanceOf[Configuration]
    //val ld = new ParticleDisperser(config)


    //  val fish = new FishConfig(ontogeny = new OntogenyConfig(1,2,3),SwimmingAbility.Passive.toString,10.0,1.3,VerticalMigrationPattern.Dial.toString,
    //  null,new PelagicLarvalDurationConfig(20,2.3,DistributionType.Normal.toString),true,99.3,10)
    //
    //  val inputFiles = new InputFilesConfig("c:\\disk","c:\\documents")
    //  val spawn = new SpawnConfig(null)
    //  val turbulence = new TurbulenceConfig(10,10,true,1)
    //  val flow = new FlowConfig(null,null,null)
    //  val habitat = new HabitatConfig(null)
    //  val output = new OutputFilesConfig(true,"line","c:\\output",)
    //
    //  val config = new Configuration()


    "A larval disperser" should "require a non-null configuration file" in {
      intercept[IllegalArgumentException] {
        new ParticleDisperser(null)
      }
    }

    it should "produce a non-null object when passed a configuration file" in {
      //assert(new ParticleDisperser(config) != null)
    }



    it should "calculate the mortality rate using the step" in {
      val calculateMortalityRate = PrivateMethod[Double]('calculateMortalityRate)
      val step = 5
      //assertResult(ld.mortality.calculateMortalityRate(step)) {
        //ld invokePrivate calculateMortalityRate(step)
      }
    }*/
}
