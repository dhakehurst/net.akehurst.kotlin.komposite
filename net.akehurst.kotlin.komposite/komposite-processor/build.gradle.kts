plugins {
}


val version_agl:String by project
val version_kotlinx:String by project

dependencies {

    "commonMainApi"(project(":komposite-api"))
    //"commonMainApi"("net.akehurst.language:type-model:${version_agl}")

    "commonMainApi"("net.akehurst.language:agl-processor:${version_agl}")
    "commonMainImplementation"("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")

}
