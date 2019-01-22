#
# Generated Makefile - do not edit!
#
# Edit the Makefile in the project folder instead (../Makefile). Each target
# has a -pre and a -post target defined where you can add customized code.
#
# This makefile implements configuration specific macros and targets.


# Environment
MKDIR=mkdir
CP=cp
GREP=grep
NM=nm
CCADMIN=CCadmin
RANLIB=ranlib
CC=gcc
CCC=g++
CXX=g++
FC=gfortran
AS=as

# Macros
CND_PLATFORM=GNU-MacOSX
CND_CONF=MacOSX-x86
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/execint.o


# C Compiler Flags
CFLAGS=-arch i386 -arch x86_64

# CC Compiler Flags
CCFLAGS=
CXXFLAGS=

# Fortran Compiler Flags
FFLAGS=

# Assembler Flags
ASFLAGS=

# Link Libraries and Options
LDLIBSOPTIONS=

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libBuildTrace.dylib

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libBuildTrace.dylib: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${LINK.c} -arch i386 -arch x86_64 -dynamiclib -install_name libBuildTrace.dylib -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libBuildTrace.dylib -Wl,-S -fPIC ${OBJECTFILES} ${LDLIBSOPTIONS} 

${OBJECTDIR}/execint.o: execint.c 
	${MKDIR} -p ${OBJECTDIR}
	$(COMPILE.c) -s -fPIC  -o ${OBJECTDIR}/execint.o execint.c

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libBuildTrace.dylib

# Subprojects
.clean-subprojects: