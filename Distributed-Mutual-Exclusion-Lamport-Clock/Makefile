# A generic makefile for a Java project.
# From http://www.makelinux.net/make3/make3-CHP-9-SECT-2
# Modified to remove errors in the source (tab vs spaces)

VERSION_NUMBER := 1.0

# Location of trees.
SOURCE_DIR  := src
OUTPUT_DIR  := classes
TEST_OUTPUT_DIR := classes/tests

# Unix tools
AWK     := awk
SED 	:= sed
FIND    := /usr/bin/find
MKDIR   := mkdir -p
RM      := rm -rf
SHELL   := /bin/bash

# Path to support tools
# TODO: Change this to /usr/lib/jvm ??
JAVA_HOME   := /usr/lib/jvm/java-6-openjdk
AXIS_HOME   := /opt/axis-1_1
TOMCAT_HOME := /opt/jakarta-tomcat-5.0.18
XERCES_HOME := /opt/xerces-1_4_4
JUNIT_HOME  := /usr/share/java
JUNIT_JAR := $(JUNIT_HOME)/junit4-4.8.2.jar
HAMCREST_JAR := $(JUNIT_HOME)/hamcrest-core-1.1.jar

# Java tools
JAVA        := $(JAVA_HOME)/bin/java
JAVAC       := $(JAVA_HOME)/bin/javac
JFLAGS      :=	-sourcepath $(SOURCE_DIR) -d $(OUTPUT_DIR) -source 1.6
RUN_FLAGS := -cp $(OUTPUT_DIR)
TEST_BUILD_FLAGS := -g -cp $(OUTPUT_DIR):$(JUNIT_JAR) -d $(TEST_OUTPUT_DIR) -Xlint
TEST_RUN_FLAGS := -cp $(OUTPUT_DIR):$(TEST_OUTPUT_DIR):$(JUNIT_JAR):$(HAMCREST_JAR) org.junit.runner.JUnitCore

# TODO: Add -g??
JVMFLAGS    := -ea                               	\
                -esa                              	\
                -Xfuture
JVM         := $(JAVA) $(JVMFLAGS)
JAR         := $(JAVA_HOME)/bin/jar
JARFLAGS    := cf
JAVADOC := $(JAVA_HOME)/bin/javadoc
JDFLAGS := -sourcepath $(SOURCE_DIR)					\
		-d $(OUTPUT_DIR)					\
		-link http://java.sun.com/products/jdk/1.4/docs/api

# Set the Java classpath
class_path := OUTPUT_DIR                \
              XERCES_JAR                \
              COMMONS_LOGGING_JAR       \
              LOG4J_JAR                 \
              JUNIT_JAR
# Set the CLASSPATH
export CLASSPATH := $(call build-classpath, $(class_path))

# all - Perform all tasks for a complete build

.PHONY: all

all: compile javadoc

# all_src_files - Temp file for holding source file list
all_src_files := $(OUTPUT_DIR)/all-src-files.txt
all_tests := $(OUTPUT_DIR)/all-tests.txt
tests := $(OUTPUT_DIR)/tests.txt

# compile - Compile the source
.PHONY: compile

compile: $(all_src_files)
	$(JAVAC) $(JFLAGS) @$<

# all_src_files - Gather source file list

# I really don't know what .INTERMEDIATE does.
# .INTERMEDIATE: $(all_src_files) $(all_tests)

.PHONY: $(all_src_files) $(all_tests)

$(all_src_files):
	$(MKDIR) $(TEST_OUTPUT_DIR)
	$(FIND) $(SOURCE_DIR) -name '*.java' > $@

# Building Unit Tests

TEST_PATH := tests

build_src_and_tests: compile build_tests

$(all_tests):
	$(FIND) $(TEST_PATH) -name '*.java' > $@
	$(SED) -e 's:^tests/::' -e 's:\.java::' -e 's:/:.:g' $@ > $(tests)

build_tests: $(all_tests)
	$(JAVAC) $(TEST_BUILD_FLAGS) @$<

# Didn't work. I wanted it such that I didn't have to rebuild
# everything all the time.
# $(JAVAC) $(TEST_BUILD_FLAGS) $(TEST_PATH)/mutexlamport/AllTests.java

test: build_src_and_tests
	cat $(tests)
	xargs -a $(tests) $(JAVA) $(TEST_RUN_FLAGS)

# javadoc - Generate the Java doc from sources

.PHONY: javadoc

# TODO: This doesn't seem to work. The command worked when I tried it by
# hand, though.
javadoc: $(all_src_files)
        echo $(JAVADOC) $(JDFLAGS) @$<

execute: compile
	cd bin/ && ./run-mutex-program.bash

.PHONY: clean

clean:
	$(RM) $(OUTPUT_DIR)

