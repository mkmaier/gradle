/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.integtests.samples.dependencymanagement

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.Sample
import org.gradle.integtests.fixtures.UsesSample
import org.gradle.test.fixtures.file.TestFile
import org.junit.Rule

class SamplesManagingTransitiveDependenciesIntegrationTest extends AbstractIntegrationSpec {

    private static final String COPY_LIBS_TASK_NAME = 'copyLibs'

    @Rule
    Sample sample = new Sample(testDirectoryProvider)

    @UsesSample("userguide/dependencyManagement/managingTransitiveDependencies/versionsWithConstraints")
    def "respects dependency constraints for direct and transitive dependencies"() {
        executer.inDirectory(sample.dir)

        when:
        succeeds(COPY_LIBS_TASK_NAME)

        then:
        sample.dir.file('build/libs/httpclient-4.5.3.jar').isFile()
        sample.dir.file('build/libs/commons-codec-1.11.jar').isFile()
    }

    @UsesSample("userguide/dependencyManagement/managingTransitiveDependencies/unresolved")
    def "reports an error for unresolved transitive dependency artifacts"() {
        executer.inDirectory(sample.dir)

        when:
        fails('compileJava')

        then:
        failure.assertHasDescription("Could not resolve all files for configuration ':compileClasspath'.")
        failure.assertHasCause("""Could not find jms.jar (javax.jms:jms:1.1).
Searched in the following locations:
    https://repo.maven.apache.org/maven2/javax/jms/jms/1.1/jms-1.1.jar""")
        failure.assertHasCause("""Could not find jmxtools.jar (com.sun.jdmk:jmxtools:1.2.1).
Searched in the following locations:
    https://repo.maven.apache.org/maven2/com/sun/jdmk/jmxtools/1.2.1/jmxtools-1.2.1.jar""")
        failure.assertHasCause("""Could not find jmxri.jar (com.sun.jmx:jmxri:1.2.1).
Searched in the following locations:
    https://repo.maven.apache.org/maven2/com/sun/jmx/jmxri/1.2.1/jmxri-1.2.1.jar""")
    }

    @UsesSample("userguide/dependencyManagement/managingTransitiveDependencies/excludeForDependency")
    def "can exclude transitive dependencies for declared dependency"() {
        executer.inDirectory(sample.dir)

        when:
        succeeds('compileJava', COPY_LIBS_TASK_NAME)

        then:
        sample.dir.file('build/classes/java/main/Main.class').isFile()
        def libs = listFilesInBuildLibsDir()
        libs.size() == 3
        libs.any { it.name == 'log4j-1.2.15.jar' || it.name == 'mail-1.4.jar' || it.name == 'activation-1.1.jar' }
    }

    @UsesSample("userguide/dependencyManagement/managingTransitiveDependencies/excludeForConfiguration")
    def "can exclude transitive dependencies for particular configuration"() {
        executer.inDirectory(sample.dir)

        when:
        succeeds('compileJava', COPY_LIBS_TASK_NAME)

        then:
        sample.dir.file('build/classes/java/main/Main.class').isFile()
        def libs = listFilesInBuildLibsDir()
        libs.size() == 3
        libs.any { it.name == 'log4j-1.2.15.jar' || it.name == 'mail-1.4.jar' || it.name == 'activation-1.1.jar' }
    }

    @UsesSample("userguide/dependencyManagement/managingTransitiveDependencies/excludeForAllConfigurations")
    def "can exclude transitive dependencies for all configurations"() {
        executer.inDirectory(sample.dir)

        when:
        succeeds('compileJava', COPY_LIBS_TASK_NAME)

        then:
        sample.dir.file('build/classes/java/main/Main.class').isFile()
        def libs = listFilesInBuildLibsDir()
        libs.size() == 3
        libs.any { it.name == 'log4j-1.2.15.jar' || it.name == 'mail-1.4.jar' || it.name == 'activation-1.1.jar' }
    }

    @UsesSample("userguide/dependencyManagement/managingTransitiveDependencies/forceForDependency")
    def "can force a dependency version"() {
        executer.inDirectory(sample.dir)

        when:
        succeeds(COPY_LIBS_TASK_NAME)

        then:
        def libs = listFilesInBuildLibsDir()
        libs.any { it.name == 'commons-codec-1.9.jar' }
        !libs.any { it.name == 'commons-codec-1.10.jar' }
    }

    @UsesSample("userguide/dependencyManagement/managingTransitiveDependencies/forceForConfiguration")
    def "can force a dependency version for particular configuration"() {
        executer.inDirectory(sample.dir)

        when:
        succeeds(COPY_LIBS_TASK_NAME)

        then:
        def libs = listFilesInBuildLibsDir()
        libs.any { it.name == 'commons-codec-1.9.jar' }
        !libs.any { it.name == 'commons-codec-1.10.jar' }
    }

    @UsesSample("userguide/dependencyManagement/managingTransitiveDependencies/disableForDependency")
    def "can disable transitive dependency resolution for dependency"() {
        executer.inDirectory(sample.dir)

        when:
        succeeds(COPY_LIBS_TASK_NAME)

        then:
        assertSingleLib('guava-23.0.jar')
    }

    @UsesSample("userguide/dependencyManagement/managingTransitiveDependencies/disableForConfiguration")
    def "can disable transitive dependency resolution for particular configuration"() {
        executer.inDirectory(sample.dir)

        when:
        succeeds(COPY_LIBS_TASK_NAME)

        then:
        assertSingleLib('guava-23.0.jar')
    }

    @UsesSample("userguide/dependencyManagement/managingTransitiveDependencies/constraintsFromBOM")
    def "can import dependency versions from a bom"() {
        executer.inDirectory(sample.dir)

        when:
        succeeds(COPY_LIBS_TASK_NAME)

        then:
        def libs = listFilesInBuildLibsDir()
        libs.findAll { it.name == 'gson-2.8.2.jar' || it.name == 'dom4j-1.6.1.jar' || it.name == 'xml-apis-1.4.01.jar'}.size() == 3
    }

    private TestFile[] listFilesInBuildLibsDir() {
        sample.dir.file('build/libs').listFiles()
    }

    private void assertSingleLib(String filename) {
        def libs = listFilesInBuildLibsDir()
        assert libs.size() == 1
        assert libs[0].name == filename
    }
}
