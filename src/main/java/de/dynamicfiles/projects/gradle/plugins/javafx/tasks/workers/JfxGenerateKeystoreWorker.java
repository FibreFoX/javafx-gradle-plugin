/*
 * Copyright 2016 Danny Althoff
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
package de.dynamicfiles.projects.gradle.plugins.javafx.tasks.workers;

import com.oracle.tools.packager.Log;
import de.dynamicfiles.projects.gradle.plugins.javafx.JavaFXGradlePluginExtension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.GradleException;
import org.gradle.api.Project;

/**
 *
 * @author Danny Althoff
 */
public class JfxGenerateKeystoreWorker extends JfxAbstractWorker {

    @FunctionalInterface
    private interface RequiredFieldAlternativeCallback {

        String getValue();
    }

    public void jfxgeneratekeystore(Project project) {
        // get our configuration
        JavaFXGradlePluginExtension ext = project.getExtensions().getByType(JavaFXGradlePluginExtension.class);
        addDeployDirToSystemClassloader(project, ext);

        // set logger-level
        Log.setLogger(new Log.Logger(ext.isVerbose()));

        File keyStore = getAbsoluteOrProjectRelativeFile(project, ext.getKeyStore(), ext.isCheckForAbsolutePaths());

        if( keyStore.exists() ){
            if( ext.isOverwriteKeyStore() ){
                if( !keyStore.delete() ){
                    throw new GradleException("Unable to delete existing keystore at: " + keyStore);
                }
            } else {
                throw new GradleException("Keystore already exists (set 'overwriteKeyStore' to force) at: " + keyStore);
            }
        }

        checkKeystoreRequiredParameter(ext.getKeyStoreAlias(), "keyStoreAlias");
        checkKeystoreRequiredParameter(ext.getKeyStorePassword(), "keyStorePassword");

        if( ext.getKeyPassword() == null ){
            ext.setKeyPassword(ext.getKeyStorePassword());
        }

        List<String> distinguishedNameParts = new ArrayList<>();

        checkAndAddRequiredField(distinguishedNameParts, "certDomain", ext.getCertDomain(), "cn");
        checkAndAddRequiredField(distinguishedNameParts, "certOrgUnit", ext.getCertOrgUnit(), "ou", () -> {
            return "none";
        });
        checkAndAddRequiredField(distinguishedNameParts, "certOrg", ext.getCertOrg(), "o");
        checkAndAddRequiredField(distinguishedNameParts, "certState", ext.getCertState(), "st");
        checkAndAddRequiredField(distinguishedNameParts, "certCountry", ext.getCertCountry(), "c");

        generateKeyStore(
                project, keyStore, ext.getKeyStoreAlias(), ext.getKeyStorePassword(), ext.getKeyPassword(), String.join(", ", distinguishedNameParts), ext.isVerbose(), ext.isUseEnvironmentRelativeExecutables()
        );
    }

    protected void generateKeyStore(Project project, File keyStore, String keyStoreAlias, String keyStorePassword, String keyPassword, String distinguishedName, boolean verbose, boolean useEnvironmentRelativeExecutables) {
        project.getLogger().info("Generating keystore in: " + keyStore);

        try{
            // generated folder if it does not exist
            Files.createDirectories(keyStore.getParentFile().toPath());

            List<String> command = new ArrayList<>();

            command.add(getEnvironmentRelativeExecutablePath(useEnvironmentRelativeExecutables) + "keytool");
            command.add("-genkeypair");
            command.add("-keystore");
            command.add(keyStore.getPath());
            command.add("-alias");
            command.add(keyStoreAlias);
            command.add("-storepass");
            command.add(keyStorePassword);
            command.add("-keypass");
            command.add(keyPassword);
            command.add("-dname");
            command.add(distinguishedName);
            command.add("-sigalg");
            command.add("SHA256withRSA");
            command.add("-validity");
            command.add("100");
            command.add("-keyalg");
            command.add("RSA");
            command.add("-keysize");
            command.add("2048");
            if( verbose ){
                command.add("-v");
            }

            ProcessBuilder pb = new ProcessBuilder();
            if( !isGradleDaemonMode() ){
                pb.inheritIO();
            }

            if( verbose ){
                project.getLogger().lifecycle("Running command: " + String.join(" ", command));
            }

            pb.command(command);
            Process p = pb.start();

            if( isGradleDaemonMode() ){
                redirectIO(p, project.getLogger());
            }

            p.waitFor();
        } catch(IOException | InterruptedException ex){
            throw new GradleException("There was an exception while generating keystore.", ex);
        }
    }

    private void checkKeystoreRequiredParameter(String value, String valueName) {
        if( value == null || value.trim().isEmpty() ){
            throw new GradleException("The property '" + valueName + "' is required to generate a new KeyStore.");
        }
    }

    private void checkAndAddRequiredField(List<String> distinguishedNameParts, String propertyName, String value, String fieldName) {
        checkAndAddRequiredField(distinguishedNameParts, propertyName, value, fieldName, null);
    }

    private void checkAndAddRequiredField(List<String> distinguishedNameParts, String propertyName, String value, String fieldName, RequiredFieldAlternativeCallback alternative) {
        if( value != null && !value.trim().isEmpty() ){
            distinguishedNameParts.add(fieldName + "=" + value);
        } else if( alternative == null || alternative.getValue() == null || alternative.getValue().trim().isEmpty() ){
            throw new GradleException("The property '" + propertyName + "' must be provided to generate a new certificate.");
        } else {
            distinguishedNameParts.add(fieldName + "=" + alternative.getValue());
        }
    }

}
