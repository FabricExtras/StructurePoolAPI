# StructurePoolAPI

Can be used to inject structures into structure pools. Popular exampe:
- Adding custom village structures to villages

Additional features:
- Maximize spawn count of the injected structure (e.g. 2 per village)


## Installation

Add this mod as dependency into your build.gradle file.

Repository
```groovy
repositories {
    maven {
        name = 'Modrinth'
        url = 'https://api.modrinth.com/maven'
        content {
            includeGroup 'maven.modrinth'
        }
    }
}
```

### Fabric workspace
```groovy
dependencies {
    modImplementation "maven.modrinth:structure_pool_api:${project.structure_pool_api_version}"
}
```

## Usage

To see how the API is used, check out the [example mod](src/testmod/java/net/testmod/TestMod.java).

Additionally, a config type is included in the API package, which can be read/written using in JSON format.