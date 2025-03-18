.PHONY: all clean doc compile

LIB_JARS=`find -L libs/ -name "*.jar" | tr [:space:] ';'`


compile:
	mkdir -p classes
	javac -encoding UTF-8 -sourcepath src/main/java -classpath $(LIB_JARS) -d classes `find -L src/ -name "*.java"`
doc:
	mkdir -p doc
	javadoc -encoding UTF-8 -sourcepath src/main/java -classpath $(LIB_JARS) -d doc kademlia

kad:
	#java -Xmx500m -cp $(LIB_JARS);classes peersim.Simulator src/main/resources/kad.cfg
	java -Xmx500m -cp "libs/*;classes" peersim.Simulator src/main/resources/kad.cfg

vrouter:
	java -Xmx500m -cp "libs/*;classes" peersim.Simulator src/main/resources/vRouter.cfg

all: compile doc run

clean:
	rm -fr classes doc


