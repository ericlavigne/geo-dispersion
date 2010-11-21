# geo-dispersion

Molina, JL, McCarty, Christopher & Eric Lavigne (2010). Utility for calculating the geographical dispersion index of personal networks collected with EgoNet. Grant: MICINN CSO2009-07057 - Perfiles del Empresariado Étnico en España (ITINERE).

## Usage

Download and run the latest executable from https://github.com/ericlavigne/geo-dispersion/downloads

Select the CSV file that contains EgoNet's raw data output. 

      ( You can learn about Egonet at http://egonet.sf.net/ )

Answer questions about the variables in that dataset, as well as about which address to use if addresses in that dataset are ambiguous. 

Select a filename for the output CSV file (make sure it ends with .csv).

## Compiling (for programmers only)

      Install git and leiningen.

      git clone git://github.com/ericlavigne/geo-dispersion.git

      cd geo-dispersion

      lein deps

      lein uberjar

      launch4j\launch4jc.exe launch4j.xml

      Shortcut when you don't need executable: lein run geo-dispersion.core main

## License

Copyright (C) 2010 - José Luis Molina, Christopher McCarty, and Eric Lavigne

Distributed under the Eclipse Public License
