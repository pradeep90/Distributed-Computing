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



# Jars

COMMONS_LOGGING_JAR   := $(AXIS_HOME)/lib/commons-logging.jar

LOG4J_JAR             := $(AXIS_HOME)/lib/log4j-1.2.8.jar

XERCES_JAR            := $(XERCES_HOME)/xerces.jar

JUNIT_JAR             := $(JUNIT_HOME)/junit.jar



# Set the Java classpath

class_path := OUTPUT_DIR                \
              XERCES_JAR                \
              COMMONS_LOGGING_JAR       \
              LOG4J_JAR                 \
              JUNIT_JAR



# space - A blank space

space := $(empty) $(empty)



# $(call build-classpath, variable-list)

define build-classpath

$(strip                                         \
  $(patsubst :%,%,                              \
    $(subst : ,:,                               \
      $(strip                                   \
        $(foreach j,$1,$(call get-file,$j):)))))

endef



# $(call get-file, variable-name)

define get-file

  $(strip                                       \
    $($1)                                       \
    $(if $(call file-exists-eval,$1),,          \
      $(warning The file referenced by variable \
                '$1' ($($1)) cannot be found)))

endef



# $(call file-exists-eval, variable-name)

define file-exists-eval

  $(strip                                       \
    $(if $($1),,$(warning '$1' has no value))   \
    $(wildcard $($1)))
endef


# $(call brief-help, makefile)

define brief-help

  $(AWK) '$$1 ~ /^[^.][-A-Za-z0-9]*:/                   \
         { print substr($$1, 1, length($$1)-1) }' $1 |  \
  sort |                                                \
  pr -T -w 80 -4

endef



# $(call file-exists, wildcard-pattern)

file-exists = $(wildcard $1)



# $(call check-file, file-list)

define check-file

  $(foreach f, $1,                              \
    $(if $(call file-exists, $($f)),,           \
      $(warning $f ($($f)) is missing)))

endef



# #(call make-temp-dir, root-opt)

define make-temp-dir

  mktemp -t $(if $1,$1,make).XXXXXXXXXX

endef



# MANIFEST_TEMPLATE - Manifest input to m4 macro processor

MANIFEST_TEMPLATE := src/manifest/manifest.mf

TMP_JAR_DIR       := $(call make-temp-dir)

TMP_MANIFEST      := $(TMP_JAR_DIR)/manifest.mf



# $(call add-manifest, jar, jar-name, manifest-file-opt)

define add-manifest

  $(RM) $(dir $(TMP_MANIFEST))

  $(MKDIR) $(dir $(TMP_MANIFEST))

  m4 --define=NAME="$(notdir $2)"                       \
     --define=IMPL_VERSION=$(VERSION_NUMBER)            \
     --define=SPEC_VERSION=$(VERSION_NUMBER)            \
     $(if $3,$3,$(MANIFEST_TEMPLATE))                   \
     > $(TMP_MANIFEST)

  $(JAR) -ufm $1 $(TMP_MANIFEST)

  $(RM) $(dir $(TMP_MANIFEST))

endef



# $(call make-jar,jar-variable-prefix)

define make-jar

  .PHONY: $1 $$($1_name)

  $1: $($1_name)

  $$($1_name):

        cd $(OUTPUT_DIR); \
        $(JAR) $(JARFLAGS) $$(notdir $$@) $$($1_packages)

        $$(call add-manifest, $$@, $$($1_name), $$($1_manifest))

endef



# Set the CLASSPATH

export CLASSPATH := $(call build-classpath, $(class_path))



# make-directories - Ensure output directory exists.

make-directories := $(shell $(MKDIR) $(OUTPUT_DIR))



# help - The default goal

.PHONY: help

help:

        @$(call brief-help, $(CURDIR)/Makefile)


############ I understand stuff from here on ############
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
	$(FIND) $(SOURCE_DIR) -name '*.java' > $@

# Building Unit Tests

TEST_PATH := tests

build_src_and_tests: compile build_tests

$(all_tests):
	$(FIND) $(TEST_PATH) -name '*.java' > $@
	$(SED) -e 's:^tests/::' -e 's:\.java::' -e 's:/:.:g' $@ > $(tests)

build_tests: $(all_tests)
	mkdir -p $(TEST_OUTPUT_DIR)
	$(JAVAC) $(TEST_BUILD_FLAGS) @$<

test: build_src_and_tests
	cat $(tests)
	xargs -a $(tests) $(JAVA) $(TEST_RUN_FLAGS)


# javadoc - Generate the Java doc from sources

.PHONY: javadoc

# TODO: This doesn't seem to work. The command worked when I tried it by
# hand, though.
javadoc: $(all_src_files)
        echo $(JAVADOC) $(JDFLAGS) @$<

.PHONY: clean

clean:
	$(RM) $(OUTPUT_DIR)

.PHONY: classpath

classpath:
        @echo CLASSPATH='$(CLASSPATH)'

.PHONY: check-config

check-config:
	@echo Checking configuration...
	$(call check-file, $(class_path) JAVA_HOME)



.PHONY: print

print:
	$(foreach v, $(V), \
	$(warning $v = $($v)))
