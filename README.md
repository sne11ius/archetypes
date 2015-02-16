Jenkins: [![Build Status](http://wasis.nu:8090/buildStatus/icon?job=archetypes dist)](http://wasis.nu:8090/job/archetypes%20dist/)
Travis CI: [![Build Status](https://travis-ci.org/sne11ius/archetypes.svg?branch=master)](https://travis-ci.org/sne11ius/archetypes)

# archetypes

This is simple search app for maven archetypes.

 - Filter by groupId, artifactId, version or description in any combination.
 - View contents for generated example project
 
## Screens

### Archetypes search

![Archetypes search](https://raw.github.com/sne11ius/archetypes/master/screenshot0.png)

### Archetypes details

![Archetypes details](https://raw.github.com/sne11ius/archetypes/master/screenshot1.png)


## How to run

 0. Download [typesave activator](https://www.playframework.com/download) and put it in you `$PATH`
 1. Clone this
 2. Setup database according `./conf/application.conf`. You can enable
    the h2 in memory settings if you're in a hurry
 3. Run `activator run` in project base - you will need to have that in your path
 4. Goto `http://localhost:9000/mit/archetypes`

## License

Yep, it's GPL v3 - get over with it ;)

Also: see the [`LICENSE`](https://raw.githubusercontent.com/sne11ius/playlog/master/LICENSE) file
