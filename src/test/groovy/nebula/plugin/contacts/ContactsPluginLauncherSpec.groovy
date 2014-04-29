/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nebula.plugin.contacts

import nebula.plugin.info.InfoPlugin
import nebula.plugin.publishing.maven.NebulaMavenPublishingPlugin
import nebula.test.IntegrationSpec
import org.gradle.BuildResult

/**
 * The contacts plugin is the uber plugin, so we're testing all the plugins together here.
 */
class ContactsPluginLauncherSpec extends IntegrationSpec {

    def pomLocation = 'build/publications/mavenJava/pom-default.xml'
    def propsLocation = 'build/manifest/info.properties'

    def 'look in pom'() {

        buildFile << """
            ${applyPlugin(ContactsPlugin)}
            ${applyPlugin(NebulaMavenPublishingPlugin)}
            ${applyPlugin(InfoPlugin)}

            apply plugin: 'nebula-publishing'
            apply plugin: 'contacts'
            contacts {
                'benny@company.com' { } // when in a contacts block everyone needs brackets
                'bobby@company.com' {
                    github 'bob1978'
                    roles 'notify', 'owner'
                }
                'billy@company.com' {
                    moniker 'Billy Bob'
                    role 'techwriter'
                }
                'downstream@netflix.com' {
                    role 'notify'
                }
            }
            contacts 'jane@company.com'
            contacts 'jack@company.com', 'john@company.com'
            """.stripIndent()

        when:
        runTasksSuccessfully('generatePomFileForMavenJavaPublication', 'writeManifestProperties')

        then: 'pom exists'
        fileExists(pomLocation)
        def pom = new XmlSlurper().parse( file(pomLocation) )

        then: 'developer section is filled in'
        def devs = pom.developers.developer
        devs.size() == 7
        devs.any { it.email.text() == 'benny@company.com' }
        devs.any { it.email.text() == 'bobby@company.com' && it.id.text() == 'bob1978' }
        devs.any { it.email.text() == 'billy@company.com' && it.name.text() == 'Billy Bob' }
        devs.any { it.email.text() == 'downstream@netflix.com' }

        then: 'tags are in the manifest'
        fileExists(propsLocation)

        when:
        def props = new Properties()
        file(propsLocation).withInputStream {
            stream -> props.load(stream)
        }

        then: 'see key in manifest'
        props['Module-Owner'] == 'benny@company.com,bobby@company.com,jane@company.com,jack@company.com,john@company.com'
        props['Module-Email'] == 'benny@company.com,bobby@company.com,downstream@netflix.com,jane@company.com,jack@company.com,john@company.com'
    }
}
