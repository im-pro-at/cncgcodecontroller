To build the program (with changes to the source):

- install version 1.8 (aka 8.0) of the `JDK`, (openJDK variants such as [Azul's Zulu JDK](https://www.azul.com/downloads/?package=jdk&show-old-builds=true#zulu) may have better support for modern systems).

- copy or hardlink `nrjavaserial-3.9.3.jar` and `jssc.jar` to the directory `./dist/lib/`

- run `ant` from the top level directory of the repo to build the changes, with a command of the form 
   ```
     ant -Dplatforms.JDK_1.8.home=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home -Dnb.internal.action.name=build jar
   ```

this will produce the output jar files in the dist directory.
