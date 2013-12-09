Android Gen Drawable Maven plugin
=================================

http://developer.android.com/guide/topics/resources/providing-resources.html

A Maven plugin that allow you to generate density dependent drawable resources from a SVG file.  

## Sample

The [Bubble Level](https://play.google.com/store/apps/details?id=net.androgames.level) application use this plugin to generate density specific application icons from a SVG file at build time.  
You can fork the project from GitHub [here](https://github.com/avianey/Level). 

## Bounding Box

SVG files use to generate density specific drawable must specify a width and height like this :

```xml
<svg
   x="0px"
   y="0px"
   width="96"
   height="96"
```
	  
This will define the bounding box of the drawable content. Everything that is drawn outside off this bounding box will no be rendered in the resulting transcoded PNG drawable. 
Inkscape provides a way to make the SVG bounding bow match the content edges. 
If you want the bounding box to be larger than the content (with extra border), you'll need to add an extra transparent shape that is larger than the content.  
  
It is preferable for your SVG file dimensions to be a multiple of 32 and adjusted to mdpi so they can be scaled to any density without rounding the bounding box.

## How to use

### Maven installation

To use it in your Android Maven projects you need to add the following repository and plugin dependency to your project pom.xml.  

The repository :  

    <repositories>  
      ...  
      <repository>  
        <id>Android Gen Drawable Maven plugin</id>  
        <url>http://avianey.github.io/androidgendrawable-maven-plugin/</url>  
      </repository>  
    </repositories>

The dependency :  

    <plugins>
      ...
      <plugin>
        <groupId>fr.avianey.modjo</groupId>
        <artifactId>androidgendrawable-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
        <configuration>
          <from>${project.basedir}/svg</from>
          <to>${project.basedir}/res</to>
          <rename>
            <level>icon</level>
          </rename>
          <createMissingDirectories>true</createMissingDirectories>
          <targetedDensities>
            <density>ldpi</density>
            <density>mdpi</density>
            <density>hdpi</density>
            <density>xhdpi</density>
          </targetedDensities>
          <fallbackDensity>mdpi</fallbackDensity>
          <skipNoDpi>true</skipNoDpi>
          <highResIcon>level</highResIcon>
        </configuration>
        <executions>
          <execution>
            <phase>initialize</phase>
            <goals>
              <goal>gen</goal>
            </goals>
          </execution>
        </executions>
      <plugin>
    </plugins>

The groupId com.github.avianey was picked to avoid conflicts with other third parties ports published in open repositories.

### Maven configuration

The plugin can be configured using the following options : 

#### from

Path to the directory that contains the SVG files to generate drawable from.  
SVG files MUST be named according the following rules :

- {name}-{density qualifier}.svg  

Generated drawable will be named :

- {name}.png  

The {density qualifier} part of the SVG file name indicates that the Bounding Box size defined in the <svg> tag of the SVG file is the target size of the generated drawable for this {density qualifier}. Generated drawable for other densities are scaled according the 3:4:6:8 scaling ratio defined in the [Supporting Multiple Screens section](http://developer.android.com/guide/practices/screens_support.html) of the Android developers site.   

#### to

Path to the Android res/ directory that contains the various drawable/ directories.

#### createMissingDirectories

"true" if you want the plugin to create missing drawable-(density)/ directories.

#### rename

Use this map to change the name of the generated drawable.

#### targetedDensities

List of the desired densities for the generated drawable.  
If not specified, a drawable is generate for each supported density qualifier.

#### fallbackDensity

The density for unqualified drawable directories.

#### skipNoDpi

"true" if you don't want to generate drawables for "nodpi" qualified drawable directories.

#### highResIcon

The name of the SVG resource to use to generate an high res icon for the Play Store.  
The SVG should have height = width.
