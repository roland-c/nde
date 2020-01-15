# Registry data harvester 

The code in this repo is POC code for harvesting data from different API's, including conversion to Linked Open Data for the Register envisioned by NDE.
It needs refactoring ...

## Running the harvesters

There are 4 harvesters available:
1) Harvester for SPARQL endpoints
2) Harvester for OAI API's
3) Harvester for CKAN API's
4) Harvester for Collectie NL (CNL) API's

Harvesters are configured in the configuration file in the conf/ directory. Variables such as output files and adresses of API's are specified in these configuration files.

The commands for running each harvester are:
sh run_CKAN.sh conf/configCKAN.parameters
sh run_OAI.sh conf/configOAI.parameters
sh run_SPARQL.sh conf/configCNL.parameters
sh run_CNL.sh conf/configCNL.parameters